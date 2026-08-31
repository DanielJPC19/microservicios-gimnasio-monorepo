package co.analisys.gimnasio;

import co.analisys.gimnasio.model.Capacidad;
import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.model.Horario;
import co.analisys.gimnasio.repository.ClaseRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ClaseRepository claseRepository;

    @Override
    public void run(String... args) throws Exception {

        // Cargar clases de ejemplo
        // entrenadorId hace referencia al id del entrenador en entrenador-microservice
        Clase clase1 = new Clase();
        clase1.setNombre("Yoga Matutino");
        clase1.setHorario(new Horario(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0)));
        clase1.setCapacidad(new Capacidad(20));
        clase1.setEntrenadorId(1L);
        claseRepository.save(clase1);

        Clase clase2 = new Clase();
        clase2.setNombre("Spinning Vespertino");
        clase2.setHorario(new Horario(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0)));
        clase2.setCapacidad(new Capacidad(15));
        clase2.setEntrenadorId(2L);
        claseRepository.save(clase2);

        System.out.println("Datos de ejemplo cargados exitosamente.");
    }
}
