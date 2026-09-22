package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionClase {
    private String claseId;
    private String nombreClase;
    private int ocupacionActual;
    private int capacidadMaxima;
    private LocalDateTime timestamp;

    public double getPorcentajeOcupacion() {
        return capacidadMaxima == 0 ? 0 : Math.round(ocupacionActual * 1000.0 / capacidadMaxima) / 10.0;
    }
}
