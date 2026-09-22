package co.analisys.gimnasio.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaConfig {

    public static final String OCUPACION_CLASES_TOPIC = "ocupacion-clases";

    // El topic también se declara en monitoreo-service (declaración idempotente):
    // si este servicio arranca primero, el topic ya existe cuando se publica la primera ocupación.
    @Bean
    public NewTopic ocupacionClasesTopic() {
        return TopicBuilder.name(OCUPACION_CLASES_TOPIC)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }
}
