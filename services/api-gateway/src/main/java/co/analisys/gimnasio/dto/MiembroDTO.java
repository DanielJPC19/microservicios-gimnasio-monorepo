package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiembroDTO {
    private Long id;
    private String nombre;
    private Object email;
    private Object fechaInscripcion;
}
