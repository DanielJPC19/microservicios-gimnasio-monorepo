package co.analisys.gimnasio.listener;

import co.analisys.gimnasio.config.RabbitMQConsumerConfig;
import co.analisys.gimnasio.dto.EquipoAveriadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EquipoAveriaListener {

    /**
     * Canal 1 del Fanout: Soporte Técnico y Mantenimiento
     */
    @RabbitListener(queues = RabbitMQConsumerConfig.EQUIPO_MANTENIMIENTO_QUEUE)
    public void recibirAlertaMantenimiento(EquipoAveriadoEvent evento) {
        log.warn("[PUB/SUB - MANTENIMIENTO TÉCNICO] Generando Ticket de Reparación Urgente");
        log.warn("   -> Equipo ID: {} | Nombre: {}", evento.getEquipoId(), evento.getNombreEquipo());
        log.warn("   -> Motivo: {} | Gravedad: {}", evento.getMotivo(), evento.getGravedad());
        log.warn("   -> Asignado a: Cuadrilla de Mantenimiento y Proveedor de Repuestos.");
    }

    /**
     * Canal 2 del Fanout: Entrenadores de Sala
     */
    @RabbitListener(queues = RabbitMQConsumerConfig.EQUIPO_ENTRENADORES_QUEUE)
    public void recibirAlertaEntrenadores(EquipoAveriadoEvent evento) {
        log.warn("[PUB/SUB - ENTRENADORES] Alerta en Sala de Entrenamiento");
        log.warn("   -> ¡Atención Instructores! El equipo '{}' (ID: {}) no está operativo.", evento.getNombreEquipo(), evento.getEquipoId());
        log.warn("   -> Favor reprogramar rutinas de alumnos que requieran esta máquina.");
    }

    /**
     * Canal 3 del Fanout: Notificación a la App de Socios
     */
    @RabbitListener(queues = RabbitMQConsumerConfig.EQUIPO_APP_SOCIOS_QUEUE)
    public void recibirAlertaAppSocios(EquipoAveriadoEvent evento) {
        log.info("[PUB/SUB - APP SOCIOS] Notificación Push a la Comunidad del Gimnasio");
        log.info("   -> Push enviado: 'Aviso: La máquina {} se encuentra temporalmente fuera de servicio por mantenimiento preventivo.'", evento.getNombreEquipo());
        log.info("   -> Fecha reporte: {}", evento.getFechaReporte());
    }
}
