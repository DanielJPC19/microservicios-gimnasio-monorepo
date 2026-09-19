package co.analisys.gimnasio.listener;

import co.analisys.gimnasio.config.RabbitMQConfig;
import co.analisys.gimnasio.dto.PagoSolicitadoEvent;
import co.analisys.gimnasio.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoDeadLetterListener {

    private final PagoService pagoService;

    /**
     * Consume la Dead Letter Queue: registra el pago como FALLIDO para revisión manual.
     * La cabecera x-death la agrega RabbitMQ e indica de qué cola viene y por qué.
     * Con pago.dlq.consumidor-activo=false el consumidor no arranca y los mensajes
     * quedan visibles en la DLQ desde el panel de RabbitMQ.
     */
    @RabbitListener(queues = RabbitMQConfig.PAGOS_DLQ, autoStartup = "${pago.dlq.consumidor-activo}")
    public void manejarPagoFallido(PagoSolicitadoEvent evento, Message message) {
        List<Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();
        String origen = "Mensaje enviado a la DLQ";
        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, ?> muerte = xDeath.get(0);
            origen = "Cola: " + muerte.get("queue") + " | Razón: " + muerte.get("reason");
        }

        log.error("================================================================================");
        log.error("[DEAD LETTER QUEUE] Pago ID: {} no pudo procesarse", evento.getPagoId());
        log.error("   -> Miembro: {} | Monto: {} | Método: {}", evento.getMiembroId(), evento.getMonto(), evento.getMetodoPago());
        log.error("   -> {}", origen);
        log.error("   -> Registrado como FALLIDO para revisión manual.");
        log.error("================================================================================");

        pagoService.marcarComoFallido(evento.getPagoId(), origen);
    }
}
