package co.analisys.gimnasio.controller;

import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.service.GimnasioService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gimnasio/entrenadores")
@RequiredArgsConstructor
public class EntrenadorController {
    // TODO: Decoupled into multiple controllers to separate logic business.
    private final GimnasioService gimnasioService;

    @PostMapping("")
    public Entrenador agregarEntrenador(@RequestBody Entrenador entrenador) {
        return gimnasioService.agregarEntrenador(entrenador);
    }

    @GetMapping("")
    public List<Entrenador> obtenerTodosEntrenadores() {
        return gimnasioService.obtenerTodosEntrenadores();
    }
}
