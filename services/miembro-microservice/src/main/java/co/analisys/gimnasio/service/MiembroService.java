package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.RabbitMQConfig;
import co.analisys.gimnasio.dto.DatosEntrenamiento;
import co.analisys.gimnasio.dto.MiembroInscritoEvent;
import co.analisys.gimnasio.dto.RegistroEntrenamientoRequest;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.repository.MiembroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiembroService {

    private final MiembroRepository miembroRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DatosEntrenamientoProducer datosEntrenamientoProducer;

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

    public DatosEntrenamiento registrarEntrenamiento(Long miembroId, RegistroEntrenamientoRequest request) {
        if (!miembroRepository.existsById(miembroId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro " + miembroId + " no encontrado");
        }
        if (request.getTipo() == null || request.getTipo().isBlank()
                || request.getDuracionMinutos() <= 0 || request.getCalorias() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tipo es obligatorio, duracionMinutos debe ser mayor que 0 y calorias no puede ser negativo");
        }

        DatosEntrenamiento datos = new DatosEntrenamiento(
                miembroId,
                request.getTipo(),
                request.getDuracionMinutos(),
                request.getCalorias(),
                LocalDateTime.now()
        );
        datosEntrenamientoProducer.publicar(datos);
        return datos;
    }

}
