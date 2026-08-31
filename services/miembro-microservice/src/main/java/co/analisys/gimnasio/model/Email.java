package co.analisys.gimnasio.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Email {

  private String email;

  public Email(String email) {
    this.email = email;
  }

  public Email() {
  }

  public Email obtenerEmail() {
    return new Email(this.email);
  }

  public Email cambiarEmail(Email newEmail) {
    return new Email(newEmail.email);
  }
}
