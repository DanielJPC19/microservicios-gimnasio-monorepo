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
}
