package co.analisys.gimnasio.controller;

import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.service.GimnasioService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gimnasio")
@RequiredArgsConstructor
public class GimnasioController {
    // TODO: Decoupled into multiple controllers to separate logic business.
    private final GimnasioService gimnasioService;

    @PostMapping("/entrenadores")
    public Entrenador agregarEntrenador(@RequestBody Entrenador entrenador) {
        return gimnasioService.agregarEntrenador(entrenador);
    }

    @PostMapping("/equipos")
    public Equipo agregarEquipo(@RequestBody Equipo equipo) {
        return gimnasioService.agregarEquipo(equipo);
    }

    @GetMapping("/entrenadores")
    public List<Entrenador> obtenerTodosEntrenadores() {
        return gimnasioService.obtenerTodosEntrenadores();
    }

    @GetMapping("/equipos")
    public List<Equipo> obtenerTodosEquipos() {
        return gimnasioService.obtenerTodosEquipos();
    }
}
