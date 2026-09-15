package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.RabbitMQConfig;
import co.analisys.gimnasio.dto.MiembroInscritoEvent;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.repository.MiembroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiembroService {

    private final MiembroRepository miembroRepository;
    private final RabbitTemplate rabbitTemplate;

    public Miembro registrarMiembro(Miembro miembro) {
        Miembro guardado = miembroRepository.save(miembro);

        try {
            MiembroInscritoEvent evento = new MiembroInscritoEvent(
                    guardado.getId(),
                    guardado.getNombre(),
                    guardado.getEmail() != null ? guardado.getEmail().getEmail() : null,
                    guardado.getFechaInscripcion() != null ? guardado.getFechaInscripcion().getFechaInscripcion() : null
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MIEMBRO_EXCHANGE,
                    RabbitMQConfig.MIEMBRO_INSCRITO_ROUTING_KEY,
                    evento
            );
            log.info("Evento MiembroInscrito publicado exitosamente en RabbitMQ: {}", evento);
        } catch (Exception e) {
            log.error("Error al publicar evento MiembroInscrito en RabbitMQ: {}", e.getMessage());
        }

        return guardado;
    }

    public List<Miembro> obtenerTodosMiembros() {
        return miembroRepository.findAll();
    }

}
