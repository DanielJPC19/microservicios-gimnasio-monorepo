package co.analisys.gimnasio.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Capacidad {

  private int capacidad;

  public Capacidad(int capacidad) {
    this.capacidad = capacidad;
  }

  public Capacidad() {
  }

  public Capacidad obtenerCapacidad() {
    return new Capacidad(this.capacidad);
  }

  public Capacidad cambiarCapacidad(Capacidad newCapacidad) {
    return new Capacidad(newCapacidad.capacidad);
  }
}