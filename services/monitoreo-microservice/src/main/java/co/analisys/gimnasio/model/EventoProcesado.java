package co.analisys.gimnasio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Historial persistente de los eventos procesados. La restricción única garantiza que un mismo
 * mensaje (topic, partición, offset) nunca se registre dos veces, aunque se reprocese tras un fallo.
 */
@Data
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"topic", "particion", "offset_kafka"}))
public class EventoProcesado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String topic;
    private int particion;
    @Column(name = "offset_kafka")
    private long offsetKafka;
    private String clave;
    @Column(length = 4000)
    private String payload;
    private Instant fechaEvento;
    private LocalDateTime fechaProcesado;
}
