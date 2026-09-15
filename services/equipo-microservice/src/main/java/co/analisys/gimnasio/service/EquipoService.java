package co.analisys.gimnasio.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import co.analisys.gimnasio.config.RabbitMQConfig;
import co.analisys.gimnasio.dto.EquipoAveriadoEvent;
import co.analisys.gimnasio.dto.ReporteAveriaRequest;
import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final RabbitTemplate rabbitTemplate;

    public Equipo agregarEquipo(Equipo equipo) {
        return equipoRepository.save(equipo);
    }

    public List<Equipo> obtenerTodosEquipos() {
        return equipoRepository.findAll();
    }

    public Equipo reportarAveria(Long id, ReporteAveriaRequest reporte) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado con id: " + id));

        // Actualizar descripción indicando el estado de avería
        equipo.setDescripcion(equipo.getDescripcion() + " [AVERÍA: " + reporte.getMotivo() + " - " + reporte.getGravedad() + "]");
        Equipo actualizado = equipoRepository.save(equipo);

        try {
            EquipoAveriadoEvent evento = new EquipoAveriadoEvent(
                    actualizado.getId(),
                    actualizado.getNombre(),
                    reporte.getMotivo(),
                    reporte.getGravedad(),
                    LocalDateTime.now()
            );

            // Publicar al Fanout Exchange (se difunde a todas las colas suscritas)
            rabbitTemplate.convertAndSend(RabbitMQConfig.EQUIPO_EVENTS_EXCHANGE, "", evento);
            log.info("Evento Fanout EquipoAveriado publicado exitosamente en RabbitMQ: {}", evento);
        } catch (Exception e) {
            log.error("Error al publicar evento EquipoAveriado en RabbitMQ: {}", e.getMessage());
        }

        return actualizado;
    }
}
