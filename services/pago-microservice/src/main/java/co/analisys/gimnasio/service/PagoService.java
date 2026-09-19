package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.RabbitMQConfig;
import co.analisys.gimnasio.dto.PagoRequest;
import co.analisys.gimnasio.dto.PagoSolicitadoEvent;
import co.analisys.gimnasio.exception.PagoInvalidoException;
import co.analisys.gimnasio.exception.PagoRechazadoException;
import co.analisys.gimnasio.model.EstadoPago;
import co.analisys.gimnasio.model.Pago;
import co.analisys.gimnasio.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    // Método de pago que la pasarela simulada siempre rechaza (para demostrar la DLQ)
    public static final String METODO_TARJETA_RECHAZADA = "TARJETA_RECHAZADA";

    private final PagoRepository pagoRepository;
    private final RabbitTemplate rabbitTemplate;

    public Pago solicitarPago(PagoRequest request) {
        Pago pago = new Pago();
        pago.setMiembroId(request.getMiembroId());
        pago.setMonto(request.getMonto());
        pago.setMetodoPago(request.getMetodoPago());
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaSolicitud(LocalDateTime.now());
        Pago guardado = pagoRepository.save(pago);

        try {
            PagoSolicitadoEvent evento = new PagoSolicitadoEvent(
                    guardado.getId(),
                    guardado.getMiembroId(),
                    guardado.getMonto(),
                    guardado.getMetodoPago()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAGOS_EXCHANGE,
                    RabbitMQConfig.PAGO_PROCESAR_ROUTING_KEY,
                    evento
            );
            log.info("Evento PagoSolicitado publicado exitosamente en RabbitMQ: {}", evento);
        } catch (Exception e) {
            log.error("Error al publicar evento PagoSolicitado en RabbitMQ: {}", e.getMessage());
            guardado.setEstado(EstadoPago.FALLIDO);
            guardado.setMotivoFallo("No se pudo encolar el pago: " + e.getMessage());
            guardado = pagoRepository.save(guardado);
        }

        return guardado;
    }

    /**
     * Procesa el pago contra la pasarela simulada. Cada invocación cuenta como un intento;
     * si lanza excepción, el contenedor de RabbitMQ decide si reintentar o enviar a la DLQ.
     */
    public void procesarPago(PagoSolicitadoEvent evento) {
        Pago pago = pagoRepository.findById(evento.getPagoId())
                .orElseThrow(() -> new PagoInvalidoException("Pago no encontrado con id: " + evento.getPagoId()));

        pago.setIntentos(pago.getIntentos() + 1);
        try {
            cobrarEnPasarela(evento);
            pago.setEstado(EstadoPago.APROBADO);
            pago.setMotivoFallo(null);
            pago.setFechaProcesamiento(LocalDateTime.now());
        } catch (RuntimeException e) {
            pago.setMotivoFallo(e.getMessage());
            throw e;
        } finally {
            pagoRepository.save(pago);
        }
    }

    public void marcarComoFallido(Long pagoId, String origen) {
        pagoRepository.findById(pagoId).ifPresentOrElse(pago -> {
            pago.setEstado(EstadoPago.FALLIDO);
            pago.setFechaProcesamiento(LocalDateTime.now());
            if (pago.getMotivoFallo() == null) {
                pago.setMotivoFallo(origen);
            }
            pagoRepository.save(pago);
        }, () -> log.warn("Pago {} recibido en la DLQ no existe en la base de datos", pagoId));
    }

    public List<Pago> obtenerTodosPagos() {
        return pagoRepository.findAll();
    }

    public List<Pago> obtenerPagosFallidos() {
        return pagoRepository.findByEstado(EstadoPago.FALLIDO);
    }

    // Pasarela de pagos simulada
    private void cobrarEnPasarela(PagoSolicitadoEvent evento) {
        if (evento.getMiembroId() == null) {
            throw new PagoInvalidoException("El pago no tiene miembro asociado");
        }
        if (evento.getMonto() == null || evento.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PagoInvalidoException("Monto inválido: " + evento.getMonto());
        }
        if (METODO_TARJETA_RECHAZADA.equalsIgnoreCase(evento.getMetodoPago())) {
            throw new PagoRechazadoException("La pasarela rechazó la transacción del pago " + evento.getPagoId());
        }
    }
}
