package co.analisys.gimnasio.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Embeddable
public class Horario {

  private LocalDateTime horario;

  public Horario(LocalDateTime horario) {
    this.horario = horario;
  }

  public Horario() {
  }

  public Horario obtenerHorario() {
    return new Horario(this.horario);
  }

  public Horario cambiarHorario(Horario newHorario) {
    return new Horario(newHorario.horario);
  }
}