package co.analisys.gimnasio.dto;

import co.analisys.gimnasio.model.Equipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipoRequest {

    @NotBlank(message = "El nombre del equipo es obligatorio")
    private String nombre;

    @NotBlank(message = "La descripción del equipo es obligatoria")
    private String descripcion;

    @Positive(message = "La cantidad de equipos debe ser mayor a 0")
    private int cantidad;

    public Equipo toEntity() {
        return new Equipo(nombre, descripcion, cantidad);
    }
}
