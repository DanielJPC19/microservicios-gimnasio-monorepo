package co.analisys.gimnasio.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

/**
 * Declara los topics que usa el monitoreo con su política de retención.
 * La retención es lo que permite la recuperación ante fallos: mientras los mensajes sigan en el log,
 * cualquier consumidor puede volver a leerlos desde su último checkpoint.
 * Los productores (clase-service, miembro-service) declaran sus topics con la misma configuración.
 */
@Configuration
public class KafkaTopicsConfig {

    public static final String OCUPACION_CLASES = "ocupacion-clases";
    public static final String DATOS_ENTRENAMIENTO = "datos-entrenamiento";
    public static final String RESUMEN_ENTRENAMIENTO = "resumen-entrenamiento";

    @Bean
    public NewTopic ocupacionClasesTopic() {
        return TopicBuilder.name(OCUPACION_CLASES)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    public NewTopic datosEntrenamientoTopic() {
        return TopicBuilder.name(DATOS_ENTRENAMIENTO)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }

    // Compactado: Kafka conserva siempre el último resumen de cada miembro, sin importar su antigüedad.
    @Bean
    public NewTopic resumenEntrenamientoTopic() {
        return TopicBuilder.name(RESUMEN_ENTRENAMIENTO)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
}
