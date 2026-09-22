package co.analisys.gimnasio.repository;

import co.analisys.gimnasio.model.Clase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClaseRepository extends JpaRepository<Clase, Long> {

    // Bloqueo pesimista: dos ingresos simultáneos no pueden leer la misma ocupación y superar la capacidad.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clase c where c.id = :id")
    Optional<Clase> findByIdForUpdate(@Param("id") Long id);
}
