package co.analisys.gimnasio.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        equipo.validarInvariantes();
        return equipoRepository.save(equipo);
    }

    public List<Equipo> obtenerTodosEquipos() {
        return equipoRepository.findAll();
    }

    public Equipo reportarAveria(Long id, ReporteAveriaRequest reporte) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipo no encontrado con id: " + id));

        equipo.registrarAveria(reporte.getMotivo(), reporte.getGravedad());
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
