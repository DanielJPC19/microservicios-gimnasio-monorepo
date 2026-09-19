package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequest {
    private Long miembroId;
    private BigDecimal monto;
    private String metodoPago; // TARJETA, EFECTIVO, TRANSFERENCIA
}
