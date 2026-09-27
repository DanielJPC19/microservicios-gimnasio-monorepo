package co.analisys.gimnasio.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.repository.EntrenadorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntrenadorService {
    private final EntrenadorRepository entrenadorRepository;

    public Entrenador agregarEntrenador(Entrenador entrenador) {
        entrenador.validarInvariantes();
        return entrenadorRepository.save(entrenador);
    }

    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorRepository.findAll();
    }

    public Entrenador obtenerEntrenadorPorId(Long id) {
        return entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrenador con ID " + id + " no encontrado"));
    }
}
