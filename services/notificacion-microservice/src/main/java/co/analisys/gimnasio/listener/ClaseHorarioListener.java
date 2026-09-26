package co.analisys.gimnasio.listener;

import co.analisys.gimnasio.config.RabbitMQConsumerConfig;
import co.analisys.gimnasio.dto.HorarioClaseCambiadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClaseHorarioListener {

    /**
     * Canal 1 del Fanout: Notificación Push a la App Móvil
     */
    @RabbitListener(queues = RabbitMQConsumerConfig.CLASE_HORARIO_APP_MOVIL_QUEUE)
    public void notificarAppMovil(HorarioClaseCambiadoEvent evento) {
        log.info("[PUB/SUB HORARIO CLASE - APP MÓVIL] Enviando notificación push a socios inscritos");
        log.info("   -> Clase '{}' (ID: {}) cambió de {} a {}",
                evento.getNombreClase(), evento.getClaseId(), evento.getHorarioAnterior(), evento.getNuevoHorario());
        log.info("   -> Motivo: {}", evento.getMotivo());
    }

    /**
     * Canal 2 del Fanout: Envío de Correos Electrónicos (Emailing)
     */
    @RabbitListener(queues = RabbitMQConsumerConfig.CLASE_HORARIO_EMAIL_QUEUE)
    public void enviarCorreosCambioHorario(HorarioClaseCambiadoEvent evento) {
        log.info("[PUB/SUB HORARIO CLASE - EMAILING] Enviando correos de actualización de calendario");
        log.info("   -> Clase: '{}' | Horario anterior: {} -> Nuevo horario: {}",
                evento.getNombreClase(), evento.getHorarioAnterior(), evento.getNuevoHorario());
        log.info("   -> Correos despachados a socios inscritos e instructor asignado.");
    }

    /**
     * Canal 3 del Fanout: Registro de Auditoría
     */
    @RabbitListener(queues = RabbitMQConsumerConfig.CLASE_HORARIO_AUDITORIA_QUEUE)
    public void registrarAuditoriaCambioHorario(HorarioClaseCambiadoEvent evento) {
        log.info("[PUB/SUB HORARIO CLASE - AUDITORÍA] Registrando evento en bitácora de auditoría");
        log.info("   -> [{}] Clase ID={} ('{}') reprogramada de {} a {} | Motivo: {}",
                evento.getFechaCambio(), evento.getClaseId(), evento.getNombreClase(),
                evento.getHorarioAnterior(), evento.getNuevoHorario(), evento.getMotivo());
    }
}
