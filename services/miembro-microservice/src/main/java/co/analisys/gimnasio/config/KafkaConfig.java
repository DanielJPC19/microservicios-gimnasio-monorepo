package co.analisys.gimnasio.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaConfig {

    public static final String DATOS_ENTRENAMIENTO_TOPIC = "datos-entrenamiento";

    // El topic también se declara en monitoreo-service (declaración idempotente).
    // Retención de 30 días: permite reprocesar varias ventanas semanales si el análisis falla.
    @Bean
    public NewTopic datosEntrenamientoTopic() {
        return TopicBuilder.name(DATOS_ENTRENAMIENTO_TOPIC)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}
