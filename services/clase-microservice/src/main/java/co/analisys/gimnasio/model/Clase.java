package co.analisys.gimnasio.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Clase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Embedded
    private Horario horario;
    @Embedded
    private Capacidad capacidad;

    private Long entrenadorId;
}
