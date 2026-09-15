package co.analisys.gimnasio.listener;

import co.analisys.gimnasio.config.RabbitMQConsumerConfig;
import co.analisys.gimnasio.dto.MiembroInscritoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MiembroInscripcionListener {

    @RabbitListener(queues = RabbitMQConsumerConfig.MIEMBRO_NOTIFICACION_QUEUE)
    public void recibirInscripcion(MiembroInscritoEvent evento) {
        log.info("================================================================================");
        log.info("[NOTIFICACIÓN ASINCRÓNICA] Nueva inscripción recibida para el miembro ID: {}", evento.getId());
        log.info("Enviando correo de bienvenida a: {} <{}>", evento.getNombre(), evento.getEmail());
        log.info("¡Bienvenido al Gimnasio! Tu membresía quedó activa a partir de: {}", evento.getFechaInscripcion());
        log.info("================================================================================");
    }
}
