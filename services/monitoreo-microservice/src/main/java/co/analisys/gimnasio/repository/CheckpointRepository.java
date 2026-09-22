package co.analisys.gimnasio.repository;

import co.analisys.gimnasio.model.Checkpoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, String> {
}
