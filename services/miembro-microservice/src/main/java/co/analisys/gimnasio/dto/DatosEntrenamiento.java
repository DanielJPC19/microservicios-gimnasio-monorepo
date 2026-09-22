package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosEntrenamiento {
    private Long miembroId;
    private String tipo;
    private int duracionMinutos;
    private int calorias;
    private LocalDateTime fecha;
}
