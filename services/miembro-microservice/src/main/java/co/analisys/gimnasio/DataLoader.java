package co.analisys.gimnasio;

import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.repository.MiembroRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final MiembroRepository miembroRepository;

    @Override
    public void run(String... args) throws Exception {
        // Cargar miembros de ejemplo
        Miembro miembro1 = new Miembro();
        miembro1.setNombre("Juan Pérez");
        miembro1.setEmail("juan@email.com");
        miembro1.setFechaInscripcion(LocalDate.now());
        miembroRepository.save(miembro1);

        Miembro miembro2 = new Miembro();
        miembro2.setNombre("María López");
        miembro2.setEmail("maria@email.com");
        miembro2.setFechaInscripcion(LocalDate.now().minusDays(30));
        miembroRepository.save(miembro2);

        System.out.println("Datos de ejemplo cargados exitosamente.");
    }
}