package co.analisys.gimnasio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class RegistroEntrenamientoRequest {
    @NotBlank(message = "El tipo de entrenamiento es obligatorio")
    private String tipo;
    @Positive(message = "La duración en minutos debe ser mayor que 0")
    private int duracionMinutos;
    @PositiveOrZero(message = "Las calorías no pueden ser negativas")
    private int calorias;
}
