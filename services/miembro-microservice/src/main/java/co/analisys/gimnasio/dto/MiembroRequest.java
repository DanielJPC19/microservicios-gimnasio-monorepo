package co.analisys.gimnasio.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import co.analisys.gimnasio.model.Email;
import co.analisys.gimnasio.model.FechaInscripcion;
import co.analisys.gimnasio.model.Miembro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiembroRequest {

    @NotBlank(message = "El nombre del miembro es obligatorio")
    private String nombre;

    @NotNull(message = "El email del miembro es obligatorio")
    private Email email;

    @JsonAlias("fechaRegistro")
    private FechaInscripcion fechaInscripcion;

    public Miembro toEntity() {
        FechaInscripcion fecha = fechaInscripcion != null
                ? fechaInscripcion
                : new FechaInscripcion(LocalDate.now());
        return new Miembro(nombre, email, fecha);
    }
}
