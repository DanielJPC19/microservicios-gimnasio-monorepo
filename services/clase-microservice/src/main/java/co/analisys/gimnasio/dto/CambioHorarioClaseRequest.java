package co.analisys.gimnasio.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioHorarioClaseRequest {
    @NotNull(message = "Debe indicar el nuevoHorario de la clase")
    private LocalDateTime nuevoHorario;
    private String motivo;
}
