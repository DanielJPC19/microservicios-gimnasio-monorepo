package co.analisys.gimnasio.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.repository.EntrenadorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntrenadorService {
    private final EntrenadorRepository entrenadorRepository;

    public Entrenador agregarEntrenador(Entrenador entrenador) {
        return entrenadorRepository.save(entrenador);
    }

    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorRepository.findAll();
    }
}
