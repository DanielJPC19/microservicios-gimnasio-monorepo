package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.KafkaStreamsConfig;
import co.analisys.gimnasio.dto.ResumenEntrenamiento;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumenEntrenamientoService {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    // Consulta interactiva al state store de Kafka Streams: devuelve las ventanas semanales más recientes del miembro.
    public List<ResumenEntrenamiento> obtenerResumenes(Long miembroId, int semanas) {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        if (kafkaStreams == null || kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El procesador de entrenamientos aún no está listo");
        }

        Instant hasta = Instant.now();
        Instant desde = hasta.minus(KafkaStreamsConfig.VENTANA.multipliedBy(semanas));
        List<ResumenEntrenamiento> resumenes = new ArrayList<>();

        try {
            ReadOnlyWindowStore<String, ResumenEntrenamiento> store = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(KafkaStreamsConfig.RESUMEN_STORE, QueryableStoreTypes.windowStore()));

            try (WindowStoreIterator<ResumenEntrenamiento> ventanas = store.fetch(String.valueOf(miembroId), desde, hasta)) {
                while (ventanas.hasNext()) {
                    KeyValue<Long, ResumenEntrenamiento> ventana = ventanas.next();
                    Instant inicio = Instant.ofEpochMilli(ventana.key);
                    resumenes.add(ventana.value.conVentana(inicio, inicio.plus(KafkaStreamsConfig.VENTANA)));
                }
            }
        } catch (InvalidStateStoreException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El state store se está restaurando, intente de nuevo");
        }

        return resumenes;
    }
}
