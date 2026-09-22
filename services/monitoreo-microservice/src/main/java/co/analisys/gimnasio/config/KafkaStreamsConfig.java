package co.analisys.gimnasio.config;

import co.analisys.gimnasio.dto.DatosEntrenamiento;
import co.analisys.gimnasio.dto.ResumenEntrenamiento;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    public static final String RESUMEN_STORE = "resumen-entrenamiento-store";
    public static final Duration VENTANA = Duration.ofDays(7);

    @Bean
    public KStream<String, DatosEntrenamiento> kStream(StreamsBuilder streamsBuilder) {
        // Los productores no envían headers de tipo, por eso cada serde fija su clase destino.
        JsonSerde<DatosEntrenamiento> datosSerde = new JsonSerde<>(DatosEntrenamiento.class).ignoreTypeHeaders().noTypeInfo();
        JsonSerde<ResumenEntrenamiento> resumenSerde = new JsonSerde<>(ResumenEntrenamiento.class).ignoreTypeHeaders().noTypeInfo();

        KStream<String, DatosEntrenamiento> stream = streamsBuilder.stream(
                KafkaTopicsConfig.DATOS_ENTRENAMIENTO, Consumed.with(Serdes.String(), datosSerde));

        // Key = miembroId: se agrega por miembro en ventanas fijas de 7 días.
        stream.groupByKey(Grouped.with(Serdes.String(), datosSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(VENTANA))
                .aggregate(
                        ResumenEntrenamiento::new,
                        (key, value, aggregate) -> aggregate.actualizar(value),
                        Materialized.<String, ResumenEntrenamiento, WindowStore<Bytes, byte[]>>as(RESUMEN_STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(resumenSerde))
                .toStream()
                // La key de salida es Windowed<String>: se vuelve a miembroId y la ventana pasa al valor.
                .map((ventana, resumen) -> KeyValue.pair(
                        ventana.key(),
                        resumen.conVentana(ventana.window().startTime(), ventana.window().endTime())))
                .to(KafkaTopicsConfig.RESUMEN_ENTRENAMIENTO, Produced.with(Serdes.String(), resumenSerde));

        return stream;
    }
}
