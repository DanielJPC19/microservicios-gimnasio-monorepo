package co.analisys.gimnasio.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClaseResponse {
    private Long id;
    private String nombre;
    private LocalDateTime horario;
    private int capacidadMaxima;
    private EntrenadorDTO entrenador;
}
