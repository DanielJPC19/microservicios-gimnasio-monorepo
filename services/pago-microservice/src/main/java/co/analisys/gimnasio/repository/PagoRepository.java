package co.analisys.gimnasio.repository;

import co.analisys.gimnasio.model.EstadoPago;
import co.analisys.gimnasio.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByEstado(EstadoPago estado);
}
