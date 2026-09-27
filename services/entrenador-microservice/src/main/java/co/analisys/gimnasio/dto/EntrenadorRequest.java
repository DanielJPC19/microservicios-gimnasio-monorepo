package co.analisys.gimnasio.dto;

import co.analisys.gimnasio.model.Entrenador;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntrenadorRequest {

    @NotBlank(message = "El nombre del entrenador es obligatorio")
    private String nombre;

    @NotBlank(message = "La especialidad del entrenador es obligatoria")
    private String especialidad;

    public Entrenador toEntity() {
        return new Entrenador(nombre, especialidad);
    }
}
