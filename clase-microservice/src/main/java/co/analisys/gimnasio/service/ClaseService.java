package co.analisys.gimnasio.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import co.analisys.gimnasio.dto.ClaseResponse;
import co.analisys.gimnasio.dto.EntrenadorDTO;
import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.repository.ClaseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final RestTemplate restTemplate;

    public Clase programarClase(Clase clase) {
        return claseRepository.save(clase);
    }

    public List<ClaseResponse> obtenerTodasClases() {
        List<Clase> clases = claseRepository.findAll();
        return clases.stream().map(this::convertirAResponse).collect(Collectors.toList());
    }

    private ClaseResponse convertirAResponse(Clase clase) {
        ClaseResponse response = new ClaseResponse();
        response.setId(clase.getId());
        response.setNombre(clase.getNombre());
        response.setHorario(clase.getHorario());
        response.setCapacidadMaxima(clase.getCapacidadMaxima());

        // Llamar a entrenador-microservice para obtener el entrenador completo
        try {
            EntrenadorDTO entrenador = restTemplate.getForObject(
                "http://localhost:8081/api/gimnasio/entrenadores/" + clase.getEntrenadorId(),
                EntrenadorDTO.class
            );
            response.setEntrenador(entrenador);
        } catch (Exception e) {
            // Si no puede conectar, retornar null o un DTO vacío
            System.out.println("Error al conectar con entrenador-microservice: " + e.getMessage());
        }

        return response;
    }
}

