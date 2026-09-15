package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipoAveriadoEvent implements Serializable {
    private Long equipoId;
    private String nombreEquipo;
    private String motivo;
    private String gravedad; // ALTA, MEDIA, BAJA
    private LocalDateTime fechaReporte;
}
