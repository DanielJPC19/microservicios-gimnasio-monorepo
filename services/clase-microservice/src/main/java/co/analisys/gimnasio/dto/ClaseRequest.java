package co.analisys.gimnasio.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import co.analisys.gimnasio.model.Capacidad;
import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.model.Horario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaseRequest {

    @NotBlank(message = "El nombre de la clase es obligatorio")
    private String nombre;

    @NotNull(message = "El horario de la clase es obligatorio")
    private Horario horario;

    @NotNull(message = "La capacidad de la clase es obligatoria")
    @JsonAlias("capacidadMaxima")
    private Capacidad capacidad;

    @NotNull(message = "El entrenadorId es obligatorio")
    @Positive(message = "El entrenadorId debe ser mayor a 0")
    private Long entrenadorId;

    public Clase toEntity() {
        return new Clase(nombre, horario, capacidad, entrenadorId);
    }
}
