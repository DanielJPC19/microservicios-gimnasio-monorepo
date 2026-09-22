package co.analisys.gimnasio.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

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
    private final OcupacionClaseProducer ocupacionClaseProducer;

    @Value("${entrenador.service.url}")
    private String entrenadorServiceUrl;

    public Clase programarClase(Clase clase) {
        Clase guardada = claseRepository.save(clase);
        // La clase nueva aparece en el dashboard de ocupación desde el inicio.
        ocupacionClaseProducer.actualizarOcupacion(
                String.valueOf(guardada.getId()),
                guardada.getNombre(),
                guardada.getOcupacionActual(),
                guardada.getCapacidad().getCapacidad());
        return guardada;
    }

    @Transactional
    public Clase registrarIngreso(Long claseId) {
        Clase clase = buscarParaActualizar(claseId);
        if (clase.getOcupacionActual() >= clase.getCapacidad().getCapacidad()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La clase " + claseId + " ya alcanzó su capacidad máxima");
        }
        clase.setOcupacionActual(clase.getOcupacionActual() + 1);
        publicarOcupacionTrasCommit(clase);
        return clase;
    }

    @Transactional
    public Clase registrarSalida(Long claseId) {
        Clase clase = buscarParaActualizar(claseId);
        if (clase.getOcupacionActual() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La clase " + claseId + " no tiene asistentes registrados");
        }
        clase.setOcupacionActual(clase.getOcupacionActual() - 1);
        publicarOcupacionTrasCommit(clase);
        return clase;
    }

    private Clase buscarParaActualizar(Long claseId) {
        return claseRepository.findByIdForUpdate(claseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase " + claseId + " no encontrada"));
    }

    // Se publica solo cuando la transacción confirma, para no emitir ocupaciones que luego se revierten.
    private void publicarOcupacionTrasCommit(Clase clase) {
        String claseId = String.valueOf(clase.getId());
        String nombreClase = clase.getNombre();
        int ocupacionActual = clase.getOcupacionActual();
        int capacidadMaxima = clase.getCapacidad().getCapacidad();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ocupacionClaseProducer.actualizarOcupacion(claseId, nombreClase, ocupacionActual, capacidadMaxima);
            }
        });
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
        response.setOcupacionActual(clase.getOcupacionActual());

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

