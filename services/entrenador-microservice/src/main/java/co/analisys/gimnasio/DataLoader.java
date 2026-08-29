package co.analisys.gimnasio;

import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.repository.EntrenadorRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final EntrenadorRepository entrenadorRepository;

    @Override
    public void run(String... args) throws Exception {

        // Cargar entrenadores de ejemplo
        Entrenador entrenador1 = new Entrenador();
        entrenador1.setNombre("Carlos Rodríguez");
        entrenador1.setEspecialidad("Yoga");
        entrenadorRepository.save(entrenador1);

        Entrenador entrenador2 = new Entrenador();
        entrenador2.setNombre("Ana Martínez");
        entrenador2.setEspecialidad("Spinning");
        entrenadorRepository.save(entrenador2);

        System.out.println("Datos de ejemplo cargados exitosamente.");
    }
}