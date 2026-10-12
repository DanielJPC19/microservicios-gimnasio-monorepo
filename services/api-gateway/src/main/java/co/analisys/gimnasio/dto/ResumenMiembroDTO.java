package co.analisys.gimnasio.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que consolida los datos de un miembro, las clases activas del gimnasio
 * y el historial de pagos utilizando el patron API Composition / Aggregator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMiembroDTO {
    private MiembroDTO miembro;
    private List<Map<String, Object>> clasesDisponibles;
    private List<Map<String, Object>> pagosRegistrados;
    private String mensaje;
}
