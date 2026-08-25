package co.analisys.gimnasio.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Clase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private LocalDateTime horario;
    private int capacidadMaxima;

    @ManyToOne
    private Entrenador entrenador;
}
