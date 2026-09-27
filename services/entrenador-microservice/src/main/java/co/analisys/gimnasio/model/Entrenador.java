package co.analisys.gimnasio.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Entrenador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String especialidad;

    public Entrenador(String nombre, String especialidad) {
        this.nombre = nombre;
        this.especialidad = especialidad;
        validarInvariantes();
    }

    public void validarInvariantes() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del entrenador es obligatorio");
        }
        if (especialidad == null || especialidad.isBlank()) {
            throw new IllegalArgumentException("La especialidad del entrenador es obligatoria");
        }
    }

    public void actualizarEspecialidad(String nuevaEspecialidad) {
        if (nuevaEspecialidad == null || nuevaEspecialidad.isBlank()) {
            throw new IllegalArgumentException("La nueva especialidad no puede estar vacía");
        }
        this.especialidad = nuevaEspecialidad;
    }
}
