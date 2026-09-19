package co.analisys.gimnasio.listener;

import co.analisys.gimnasio.config.RabbitMQConfig;
import co.analisys.gimnasio.dto.PagoSolicitadoEvent;
import co.analisys.gimnasio.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoProcesamientoListener {

    private final PagoService pagoService;

    @RabbitListener(queues = RabbitMQConfig.PAGOS_QUEUE)
    public void procesarPago(PagoSolicitadoEvent evento) {
        log.info("[PAGOS] Procesando pago ID: {} | Miembro: {} | Monto: {} | Método: {}",
                evento.getPagoId(), evento.getMiembroId(), evento.getMonto(), evento.getMetodoPago());
        try {
            pagoService.procesarPago(evento);
            log.info("[PAGOS] Pago ID: {} APROBADO", evento.getPagoId());
        } catch (RuntimeException e) {
            log.warn("[PAGOS] Falló el procesamiento del pago ID: {} -> {}", evento.getPagoId(), e.getMessage());
            throw e;
        }
    }
}
