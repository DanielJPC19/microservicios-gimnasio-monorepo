package co.analisys.gimnasio.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.repository.EquipoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipoService {

    private final EquipoRepository equipoRepository;

    public Equipo agregarEquipo(Equipo equipo) {
        return equipoRepository.save(equipo);
    }

    public List<Equipo> obtenerTodosEquipos() {
        return equipoRepository.findAll();
    }
}
