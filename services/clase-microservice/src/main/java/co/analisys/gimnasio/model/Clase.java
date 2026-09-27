package co.analisys.gimnasio.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Clase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Embedded
    private Horario horario;
    @Embedded
    private Capacidad capacidad;

    private Long entrenadorId;

    private int ocupacionActual;

    public Clase(String nombre, Horario horario, Capacidad capacidad, Long entrenadorId) {
        this.nombre = nombre;
        this.horario = horario;
        this.capacidad = capacidad;
        this.entrenadorId = entrenadorId;
        this.ocupacionActual = 0;
        validarInvariantes();
    }

    public void validarInvariantes() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la clase es obligatorio");
        }
        if (horario == null) {
            throw new IllegalArgumentException("El horario de la clase es obligatorio");
        }
        if (capacidad == null) {
            throw new IllegalArgumentException("La capacidad de la clase es obligatoria");
        }
        if (entrenadorId == null || entrenadorId <= 0) {
            throw new IllegalArgumentException("El entrenadorId debe ser un identificador válido mayor a 0");
        }
    }

    public void reprogramar(Horario nuevoHorario) {
        if (nuevoHorario == null) {
            throw new IllegalArgumentException("El nuevo horario de la clase es obligatorio");
        }
        this.horario = this.horario != null ? this.horario.cambiarHorario(nuevoHorario) : nuevoHorario;
    }

    public void registrarIngreso() {
        if (this.capacidad == null || !this.capacidad.tieneCupoDisponible(this.ocupacionActual)) {
            throw new IllegalStateException("La clase " + this.id + " ya alcanzó su capacidad máxima");
        }
        this.ocupacionActual++;
    }

    public void registrarSalida() {
        if (this.ocupacionActual <= 0) {
            throw new IllegalStateException("La clase " + this.id + " no tiene asistentes registrados");
        }
        this.ocupacionActual--;
    }
}
