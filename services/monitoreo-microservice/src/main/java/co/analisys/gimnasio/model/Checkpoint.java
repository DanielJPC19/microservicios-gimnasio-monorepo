package co.analisys.gimnasio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Último offset procesado por partición. Se guarda en la misma transacción que el resultado del procesamiento.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {
    // "<topic>-<partición>"
    @Id
    private String id;
    private String topic;
    private int particion;
    @Column(name = "ultimo_offset")
    private long ultimoOffset;
    private LocalDateTime actualizado;
}
