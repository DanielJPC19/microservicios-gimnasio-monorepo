package co.analisys.gimnasio;

import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.repository.ClaseRepository;
import co.analisys.gimnasio.repository.EntrenadorRepository;
import co.analisys.gimnasio.repository.EquipoRepository;
import co.analisys.gimnasio.repository.MiembroRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final MiembroRepository miembroRepository;
    private final ClaseRepository claseRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EquipoRepository equipoRepository;

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

        // Cargar entrenadores de ejemplo
        Entrenador entrenador1 = new Entrenador();
        entrenador1.setNombre("Carlos Rodríguez");
        entrenador1.setEspecialidad("Yoga");
        entrenadorRepository.save(entrenador1);

        Entrenador entrenador2 = new Entrenador();
        entrenador2.setNombre("Ana Martínez");
        entrenador2.setEspecialidad("Spinning");
        entrenadorRepository.save(entrenador2);

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

        // Cargar equipos de ejemplo
        Equipo equipo1 = new Equipo();
        equipo1.setNombre("Mancuernas");
        equipo1.setDescripcion("Set de mancuernas de 5kg");
        equipo1.setCantidad(20);
        equipoRepository.save(equipo1);

        Equipo equipo2 = new Equipo();
        equipo2.setNombre("Bicicleta estática");
        equipo2.setDescripcion("Bicicleta para spinning");
        equipo2.setCantidad(15);
        equipoRepository.save(equipo2);

        System.out.println("Datos de ejemplo cargados exitosamente.");
    }
}