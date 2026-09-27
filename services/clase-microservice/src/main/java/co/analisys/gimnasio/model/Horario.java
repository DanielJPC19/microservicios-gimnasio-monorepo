package co.analisys.gimnasio.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class Horario {

    private LocalDateTime horario;

    public Horario(LocalDateTime horario) {
        if (horario == null) {
            throw new IllegalArgumentException("El horario de la clase no puede ser nulo");
        }
        this.horario = horario;
    }

    protected Horario() {
        // Requerido por JPA
    }

    @JsonValue
    public LocalDateTime getHorario() {
        return horario;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Horario fromJson(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("El horario de la clase es obligatorio");
        }
        if (value instanceof LocalDateTime ldt) {
            return new Horario(ldt);
        }
        if (value instanceof String str) {
            if (str.isBlank()) {
                throw new IllegalArgumentException("El horario de la clase no puede estar vacío");
            }
            try {
                return new Horario(LocalDateTime.parse(str.trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("El formato del horario debe ser ISO-8601 (ej. 2026-09-01T10:00:00)");
            }
        }
        if (value instanceof Map<?, ?> map) {
            Object raw = map.get("horario");
            return fromJson(raw);
        }
        throw new IllegalArgumentException("Formato de horario inválido");
    }

    public Horario obtenerHorario() {
        return new Horario(this.horario);
    }

    public Horario cambiarHorario(Horario newHorario) {
        if (newHorario == null) {
            throw new IllegalArgumentException("El nuevo horario no puede ser nulo");
        }
        return new Horario(newHorario.horario);
    }
}