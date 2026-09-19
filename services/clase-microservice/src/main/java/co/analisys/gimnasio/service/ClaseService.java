package co.analisys.gimnasio.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import co.analisys.gimnasio.dto.ClaseResponse;
import co.analisys.gimnasio.dto.EntrenadorDTO;
import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.repository.ClaseRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final RestTemplate restTemplate;

    @Value("${entrenador.service.url}")
    private String entrenadorServiceUrl;

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
        response.setHorario(clase.getHorario().getHorario());
        response.setCapacidadMaxima(clase.getCapacidad().getCapacidad());

        try {
            HttpHeaders headers = new HttpHeaders();
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest httpRequest = attrs.getRequest();
                String authorization = httpRequest.getHeader("Authorization");
                if (authorization != null) {
                    headers.set("Authorization", authorization);
                }
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<EntrenadorDTO> entrenadorResponse = restTemplate.exchange(
                entrenadorServiceUrl + "/api/gimnasio/entrenadores/" + clase.getEntrenadorId(),
                HttpMethod.GET,
                entity,
                EntrenadorDTO.class
            );

            if (entrenadorResponse.getBody() != null) {
                response.setEntrenador(entrenadorResponse.getBody());
            }
        } catch (Exception e) {
            System.out.println("Error al conectar con entrenador-microservice: " + e.getMessage());
        }

        return response;
    }
}

