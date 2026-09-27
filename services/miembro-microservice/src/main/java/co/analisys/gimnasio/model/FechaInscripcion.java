package co.analisys.gimnasio.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class FechaInscripcion {

    private LocalDate fechaInscripcion;

    public FechaInscripcion(LocalDate fechaInscripcion) {
        if (fechaInscripcion == null) {
            throw new IllegalArgumentException("La fecha de inscripción no puede ser nula");
        }
        this.fechaInscripcion = fechaInscripcion;
    }

    protected FechaInscripcion() {
        // Requerido por JPA
    }

    @JsonValue
    public LocalDate getFechaInscripcion() {
        return fechaInscripcion;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FechaInscripcion fromJson(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("La fecha de inscripción es obligatoria");
        }
        if (value instanceof LocalDate ld) {
            return new FechaInscripcion(ld);
        }
        if (value instanceof String str) {
            if (str.isBlank()) {
                throw new IllegalArgumentException("La fecha de inscripción no puede estar vacía");
            }
            try {
                return new FechaInscripcion(LocalDate.parse(str.trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("El formato de fechaInscripcion debe ser YYYY-MM-DD (ej. 2026-08-31)");
            }
        }
        if (value instanceof Map<?, ?> map) {
            Object raw = map.containsKey("fechaInscripcion") ? map.get("fechaInscripcion") : map.get("fechaRegistro");
            return fromJson(raw);
        }
        throw new IllegalArgumentException("Formato de fecha de inscripción inválido");
    }

    public FechaInscripcion obtenerFechaInscripcion() {
        return new FechaInscripcion(this.fechaInscripcion);
    }

    public FechaInscripcion cambiarFechaInscripcion(FechaInscripcion newFechaInscripcion) {
        if (newFechaInscripcion == null) {
            throw new IllegalArgumentException("La nueva fecha de inscripción no puede ser nula");
        }
        return new FechaInscripcion(newFechaInscripcion.fechaInscripcion);
    }
}
