package co.analisys.gimnasio.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioHorarioClaseRequest {
    private LocalDateTime nuevoHorario;
    private String motivo;
}
