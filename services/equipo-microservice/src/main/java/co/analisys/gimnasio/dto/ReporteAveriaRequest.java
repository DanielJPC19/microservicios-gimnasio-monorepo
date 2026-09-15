package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAveriaRequest {
    private String motivo;
    private String gravedad; // ALTA, MEDIA, BAJA
}
