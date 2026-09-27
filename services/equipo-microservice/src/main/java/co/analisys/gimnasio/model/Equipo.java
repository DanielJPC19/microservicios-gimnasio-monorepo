package co.analisys.gimnasio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Equipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Column(length = 1000)
    private String descripcion;
    private int cantidad;

    public Equipo(String nombre, String descripcion, int cantidad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        validarInvariantes();
    }

    public void validarInvariantes() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción del equipo es obligatoria");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de equipos debe ser mayor a 0");
        }
    }

    public void registrarAveria(String motivo, String gravedad) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de la avería es obligatorio");
        }
        String nivelGravedad = (gravedad != null && !gravedad.isBlank()) ? gravedad : "MEDIA";
        String baseDesc = this.descripcion != null ? this.descripcion : "";
        int idx = baseDesc.indexOf(" [AVERÍA:");
        if (idx != -1) {
            baseDesc = baseDesc.substring(0, idx);
        }
        this.descripcion = baseDesc + " [AVERÍA: " + motivo + " - " + nivelGravedad + "]";
    }
}
