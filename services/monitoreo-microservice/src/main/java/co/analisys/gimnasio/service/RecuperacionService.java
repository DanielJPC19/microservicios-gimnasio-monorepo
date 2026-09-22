package co.analisys.gimnasio.service;

import co.analisys.gimnasio.config.KafkaTopicsConfig;
import co.analisys.gimnasio.model.Checkpoint;
import co.analisys.gimnasio.model.EventoProcesado;
import co.analisys.gimnasio.repository.CheckpointRepository;
import co.analisys.gimnasio.repository.EventoProcesadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procesa los topics del gimnasio con checkpoints propios para poder recuperarse ante fallos.
 *
 * - Los offsets NO se confirman en Kafka (enable.auto.commit=false): la fuente de verdad es la tabla Checkpoint.
 * - Cada registro se procesa y su offset se guarda en la MISMA transacción: o quedan ambos o ninguno,
 *   así un fallo a mitad nunca pierde ni duplica un evento.
 * - Al asignarse una partición (arranque o rebalanceo) se hace seek al último checkpoint + 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecuperacionService implements SmartLifecycle {

    private static final List<String> TOPICS = List.of(
            KafkaTopicsConfig.OCUPACION_CLASES,
            KafkaTopicsConfig.DATOS_ENTRENAMIENTO);
    private static final Duration PAUSA_TRAS_FALLO = Duration.ofSeconds(1);

    private final CheckpointRepository checkpointRepository;
    private final EventoProcesadoRepository eventoProcesadoRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${recuperacion.group-id:recuperacion-grupo}")
    private String groupId;

    @Value("${recuperacion.activo:true}")
    private boolean activo;

    private volatile boolean running;
    private KafkaConsumer<String, String> consumer;
    private Thread hilo;

    @Override
    public void start() {
        if (!activo) {
            log.info("[RECUPERACION] Procesamiento con checkpoints desactivado (recuperacion.activo=false)");
            return;
        }
        consumer = new KafkaConsumer<>(propiedadesConsumidor());
        running = true;
        // KafkaConsumer no es thread-safe: todo el uso del consumidor ocurre en este hilo.
        hilo = new Thread(this::iniciarProcesamiento, "recuperacion-kafka");
        hilo.start();
    }

    @Override
    public void stop() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
        if (hilo != null) {
            try {
                hilo.join(Duration.ofSeconds(10).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void iniciarProcesamiento() {
        try {
            consumer.subscribe(TOPICS, new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> particiones) {
                    reanudarDesdeCheckpoint(particiones);
                }

                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> particiones) {
                    log.info("[RECUPERACION] Particiones revocadas: {}", particiones);
                }
            });

            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (TopicPartition particion : records.partitions()) {
                    procesarParticion(particion, records.records(particion));
                }
            }
        } catch (WakeupException e) {
            // Señal de apagado lanzada por stop().
            if (running) {
                throw e;
            }
        } catch (Exception e) {
            log.error("[RECUPERACION] El procesamiento se detuvo por un error inesperado", e);
        } finally {
            consumer.close();
            running = false;
            log.info("[RECUPERACION] Consumidor cerrado");
        }
    }

    private void procesarParticion(TopicPartition particion, List<ConsumerRecord<String, String>> registros) {
        for (ConsumerRecord<String, String> record : registros) {
            try {
                transactionTemplate.executeWithoutResult(estado -> {
                    procesarRecord(record);
                    guardarOffset(record.topic(), record.partition(), record.offset());
                });
            } catch (Exception e) {
                // La transacción se revirtió: se vuelve a este offset y el siguiente poll lo reintenta.
                // El resto de registros de esta partición se descartan del lote para no saltarse ninguno.
                log.error("[RECUPERACION] Fallo procesando {}@{}, se reintentará: {}", particion, record.offset(), e.getMessage());
                consumer.seek(particion, record.offset());
                pausar();
                return;
            }
        }
    }

    private void reanudarDesdeCheckpoint(Collection<TopicPartition> particiones) {
        Map<TopicPartition, Long> ultimoOffsetProcesado = cargarUltimoOffset(particiones);
        for (TopicPartition particion : particiones) {
            Long ultimoOffset = ultimoOffsetProcesado.get(particion);
            if (ultimoOffset != null) {
                log.info("[RECUPERACION] Reanudando {} desde el offset {} (último checkpoint: {})",
                        particion, ultimoOffset + 1, ultimoOffset);
                consumer.seek(particion, ultimoOffset + 1);
            } else {
                log.info("[RECUPERACION] Sin checkpoint para {}, procesando desde el inicio del log", particion);
                consumer.seekToBeginning(List.of(particion));
            }
        }
    }

    private Map<TopicPartition, Long> cargarUltimoOffset(Collection<TopicPartition> particiones) {
        Map<TopicPartition, Long> offsets = new HashMap<>();
        for (TopicPartition particion : particiones) {
            checkpointRepository.findById(idCheckpoint(particion.topic(), particion.partition()))
                    .ifPresent(checkpoint -> offsets.put(particion, checkpoint.getUltimoOffset()));
        }
        return offsets;
    }

    private void procesarRecord(ConsumerRecord<String, String> record) {
        EventoProcesado evento = new EventoProcesado();
        evento.setTopic(record.topic());
        evento.setParticion(record.partition());
        evento.setOffsetKafka(record.offset());
        evento.setClave(record.key());
        evento.setPayload(record.value());
        evento.setFechaEvento(Instant.ofEpochMilli(record.timestamp()));
        evento.setFechaProcesado(LocalDateTime.now());
        eventoProcesadoRepository.save(evento);
        log.info("[RECUPERACION] Procesado {}-{}@{} key={}", record.topic(), record.partition(), record.offset(), record.key());
    }

    private void guardarOffset(String topic, int partition, long offset) {
        checkpointRepository.save(new Checkpoint(idCheckpoint(topic, partition), topic, partition, offset, LocalDateTime.now()));
    }

    public List<Checkpoint> obtenerCheckpoints() {
        return checkpointRepository.findAll();
    }

    public Map<String, Long> contarEventosPorTopic() {
        Map<String, Long> conteo = new HashMap<>();
        TOPICS.forEach(topic -> conteo.put(topic, eventoProcesadoRepository.countByTopic(topic)));
        return conteo;
    }

    private static String idCheckpoint(String topic, int partition) {
        return topic + "-" + partition;
    }

    private Map<String, Object> propiedadesConsumidor() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Si el checkpoint apunta a un offset ya eliminado por la retención, se continúa desde el más antiguo disponible.
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return props;
    }

    private void pausar() {
        try {
            Thread.sleep(PAUSA_TRAS_FALLO.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
