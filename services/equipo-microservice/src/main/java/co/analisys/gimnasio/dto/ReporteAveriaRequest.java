package co.analisys.gimnasio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAveriaRequest {
    @NotBlank(message = "El motivo de la avería es obligatorio")
    private String motivo;
    @NotBlank(message = "La gravedad de la avería es obligatoria")
    private String gravedad; // ALTA, MEDIA, BAJA
}
