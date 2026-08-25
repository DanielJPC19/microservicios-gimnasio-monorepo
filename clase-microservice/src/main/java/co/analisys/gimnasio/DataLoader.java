package co.analisys.gimnasio;

import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.repository.ClaseRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ClaseRepository claseRepository;

    @Override
    public void run(String... args) throws Exception {

        // Cargar clases de ejemplo
        Clase clase1 = new Clase();
        clase1.setNombre("Yoga Matutino");
        clase1.setHorario(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));
        clase1.setCapacidadMaxima(20);
        clase1.setEntrenador(entrenador1);
        claseRepository.save(clase1);

        Clase clase2 = new Clase();
        clase2.setNombre("Spinning Vespertino");
        clase2.setHorario(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0));
        clase2.setCapacidadMaxima(15);
        clase2.setEntrenador(entrenador2);
        claseRepository.save(clase2);

        System.out.println("Datos de ejemplo cargados exitosamente.");
    }
}