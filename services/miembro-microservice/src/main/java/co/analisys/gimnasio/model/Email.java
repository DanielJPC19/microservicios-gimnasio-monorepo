package co.analisys.gimnasio.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private String email;

    public Email(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email del miembro es obligatorio");
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El email no tiene un formato válido: " + email);
        }
        this.email = trimmed;
    }

    protected Email() {
        // Requerido por JPA
    }

    @JsonValue
    public String getEmail() {
        return email;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Email fromJson(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("El email del miembro es obligatorio");
        }
        if (value instanceof String str) {
            return new Email(str);
        }
        if (value instanceof Map<?, ?> map) {
            Object raw = map.get("email");
            return fromJson(raw);
        }
        throw new IllegalArgumentException("Formato de email inválido");
    }

    public Email obtenerEmail() {
        return new Email(this.email);
    }

    public Email cambiarEmail(Email newEmail) {
        if (newEmail == null) {
            throw new IllegalArgumentException("El nuevo email no puede ser nulo");
        }
        return new Email(newEmail.email);
    }
}
