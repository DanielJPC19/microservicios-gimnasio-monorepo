package co.analisys.gimnasio.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class Pago {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long miembroId;
  private BigDecimal monto;
  private String metodoPago; // TARJETA, EFECTIVO, TRANSFERENCIA
  @Enumerated(EnumType.STRING)
  private EstadoPago estado;
  private int intentos;
  private String motivoFallo;
  private LocalDateTime fechaSolicitud;
  private LocalDateTime fechaProcesamiento;
}
