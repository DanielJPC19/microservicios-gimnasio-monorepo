package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.KafkaConfig;
import co.analisys.gimnasio.dto.OcupacionClase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcupacionClaseProducer {

    private final KafkaTemplate<String, OcupacionClase> kafkaTemplate;

    public void actualizarOcupacion(String claseId, String nombreClase, int ocupacionActual, int capacidadMaxima) {
        OcupacionClase ocupacion = new OcupacionClase(claseId, nombreClase, ocupacionActual, capacidadMaxima, LocalDateTime.now());

        try {
            // claseId como key: todas las actualizaciones de una clase van a la misma partición y conservan su orden.
            kafkaTemplate.send(KafkaConfig.OCUPACION_CLASES_TOPIC, claseId, ocupacion)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("Error al publicar ocupación de la clase {} en Kafka: {}", claseId, error.getMessage());
                        } else {
                            log.info("Ocupación publicada en Kafka: {} (partición {}, offset {})",
                                    ocupacion,
                                    resultado.getRecordMetadata().partition(),
                                    resultado.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Error al publicar ocupación de la clase {} en Kafka: {}", claseId, e.getMessage());
        }
    }
}
