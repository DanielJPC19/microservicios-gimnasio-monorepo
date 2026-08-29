package co.analisys.gimnasio.repository;

import co.analisys.gimnasio.model.Clase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
}
