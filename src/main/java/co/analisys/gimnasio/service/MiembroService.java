package co.analisys.gimnasio.service;

import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.repository.ClaseRepository;
import co.analisys.gimnasio.repository.EntrenadorRepository;
import co.analisys.gimnasio.repository.EquipoRepository;
import co.analisys.gimnasio.repository.MiembroRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MiembroService {

    private final MiembroRepository miembroRepository;

    public Miembro registrarMiembro(Miembro miembro) {
        return miembroRepository.save(miembro);
    }

    public List<Miembro> obtenerTodosMiembros() {
        return miembroRepository.findAll();
    }

}
