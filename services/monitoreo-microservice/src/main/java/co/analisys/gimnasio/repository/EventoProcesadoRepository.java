package co.analisys.gimnasio.repository;

import co.analisys.gimnasio.model.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, Long> {

    long countByTopic(String topic);
}
