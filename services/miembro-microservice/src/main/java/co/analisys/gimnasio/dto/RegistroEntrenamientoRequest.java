package co.analisys.gimnasio.dto;

import lombok.Data;

@Data
public class RegistroEntrenamientoRequest {
    private String tipo;
    private int duracionMinutos;
    private int calorias;
}
