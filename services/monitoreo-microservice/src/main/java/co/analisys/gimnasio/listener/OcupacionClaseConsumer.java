package co.analisys.gimnasio.listener;

import co.analisys.gimnasio.config.KafkaTopicsConfig;
import co.analisys.gimnasio.dto.OcupacionClase;
import co.analisys.gimnasio.service.OcupacionDashboardService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OcupacionClaseConsumer extends AbstractConsumerSeekAware {

    private final OcupacionDashboardService dashboardService;

    // El dashboard vive en memoria: al arrancar se reconstruye releyendo el log retenido del topic.
    // Como la key es claseId y el orden por partición se conserva, la última lectura de cada clase es su estado actual.
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        callback.seekToBeginning(assignments.keySet());
    }

    @KafkaListener(topics = KafkaTopicsConfig.OCUPACION_CLASES, groupId = "monitoreo-grupo")
    public void consumirActualizacionOcupacion(OcupacionClase ocupacion) {
        dashboardService.actualizarDashboard(ocupacion);
    }
}
