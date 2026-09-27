package co.analisys.gimnasio.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
public class Miembro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Embedded
    private Email email;
    @Embedded
    private FechaInscripcion fechaInscripcion;

    public Miembro(String nombre, Email email, FechaInscripcion fechaInscripcion) {
        this.nombre = nombre;
        this.email = email;
        this.fechaInscripcion = fechaInscripcion != null
                ? fechaInscripcion
                : new FechaInscripcion(LocalDate.now());
        validarInvariantes();
    }

    public void validarInvariantes() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del miembro es obligatorio");
        }
        if (email == null) {
            throw new IllegalArgumentException("El email del miembro es obligatorio");
        }
        if (fechaInscripcion == null) {
            this.fechaInscripcion = new FechaInscripcion(LocalDate.now());
        }
    }

    public void actualizarEmail(Email nuevoEmail) {
        if (nuevoEmail == null) {
            throw new IllegalArgumentException("El nuevo email es obligatorio");
        }
        this.email = this.email != null ? this.email.cambiarEmail(nuevoEmail) : nuevoEmail;
    }
}
