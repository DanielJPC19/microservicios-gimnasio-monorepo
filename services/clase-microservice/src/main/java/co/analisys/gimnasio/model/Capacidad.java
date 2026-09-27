package co.analisys.gimnasio.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
@Embeddable
public class Capacidad {

    private int capacidad;

    public Capacidad(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad de la clase debe ser mayor a 0");
        }
        this.capacidad = capacidad;
    }

    protected Capacidad() {
        // Requerido por JPA
    }

    @JsonValue
    public int getCapacidad() {
        return capacidad;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Capacidad fromJson(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("La capacidad de la clase es obligatoria");
        }
        if (value instanceof Number number) {
            return new Capacidad(number.intValue());
        }
        if (value instanceof String str) {
            try {
                return new Capacidad(Integer.parseInt(str.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La capacidad debe ser un número entero válido");
            }
        }
        if (value instanceof Map<?, ?> map) {
            Object raw = map.containsKey("capacidad") ? map.get("capacidad") : map.get("capacidadMaxima");
            return fromJson(raw);
        }
        throw new IllegalArgumentException("Formato de capacidad inválido");
    }

    public boolean tieneCupoDisponible(int ocupacionActual) {
        return ocupacionActual < this.capacidad;
    }

    public Capacidad obtenerCapacidad() {
        return new Capacidad(this.capacidad);
    }

    public Capacidad cambiarCapacidad(Capacidad newCapacidad) {
        if (newCapacidad == null) {
            throw new IllegalArgumentException("La nueva capacidad no puede ser nula");
        }
        return new Capacidad(newCapacidad.capacidad);
    }
}