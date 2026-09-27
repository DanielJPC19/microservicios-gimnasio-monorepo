package co.analisys.gimnasio.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioClaseCambiadoEvent implements Serializable {
    private Long claseId;
    private String nombreClase;
    private LocalDateTime horarioAnterior;
    private LocalDateTime nuevoHorario;
    private String motivo;
    private LocalDateTime fechaCambio;
}
