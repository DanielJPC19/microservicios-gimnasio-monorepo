package co.analisys.gimnasio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoSolicitadoEvent implements Serializable {
    private Long pagoId;
    private Long miembroId;
    private BigDecimal monto;
    private String metodoPago;
}
