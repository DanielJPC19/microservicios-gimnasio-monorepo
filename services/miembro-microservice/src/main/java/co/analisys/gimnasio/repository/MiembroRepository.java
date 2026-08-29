package co.analisys.gimnasio.repository;

import co.analisys.gimnasio.model.Miembro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiembroRepository extends JpaRepository<Miembro, Long> {
}
