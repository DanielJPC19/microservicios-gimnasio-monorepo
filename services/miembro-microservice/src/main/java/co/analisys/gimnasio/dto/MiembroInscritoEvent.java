package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiembroInscritoEvent implements Serializable {
    private Long id;
    private String nombre;
    private String email;
    private LocalDate fechaInscripcion;
}
