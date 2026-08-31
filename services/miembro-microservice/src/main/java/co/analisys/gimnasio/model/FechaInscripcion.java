
package co.analisys.gimnasio.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Data
@Embeddable
public class FechaInscripcion {

  private LocalDate fechaInscripcion;

  public FechaInscripcion(LocalDate fechaInscripcion) {
    this.fechaInscripcion = fechaInscripcion;
  }

  public FechaInscripcion() {
  }

  public FechaInscripcion obtenerFechaInscripcion() {
    return new FechaInscripcion(this.fechaInscripcion);
  }

  public FechaInscripcion cambiarFechaInscripcion(FechaInscripcion newFechaInscripcion) {
    return new FechaInscripcion(newFechaInscripcion.fechaInscripcion);
  }
}
