package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.KafkaConfig;
import co.analisys.gimnasio.dto.DatosEntrenamiento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatosEntrenamientoProducer {

    private final KafkaTemplate<String, DatosEntrenamiento> kafkaTemplate;

    // Envío síncrono: el topic es el único lugar donde se guarda el entrenamiento,
    // así que el cliente debe enterarse si no quedó registrado.
    public void publicar(DatosEntrenamiento datos) {
        try {
            SendResult<String, DatosEntrenamiento> resultado = kafkaTemplate
                    .send(KafkaConfig.DATOS_ENTRENAMIENTO_TOPIC, String.valueOf(datos.getMiembroId()), datos)
                    .get(10, TimeUnit.SECONDS);
            log.info("Datos de entrenamiento publicados en Kafka: {} (partición {}, offset {})",
                    datos,
                    resultado.getRecordMetadata().partition(),
                    resultado.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Publicación interrumpida");
        } catch (Exception e) {
            log.error("Error al publicar datos de entrenamiento en Kafka: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo registrar el entrenamiento, intente de nuevo");
        }
    }
}
