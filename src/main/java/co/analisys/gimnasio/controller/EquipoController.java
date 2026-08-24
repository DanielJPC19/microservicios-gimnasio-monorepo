package co.analisys.gimnasio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.service.GimnasioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gimnasio/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final GimnasioService gimnasioService;

    @PostMapping("")
    public Equipo agregarEquipo(@RequestBody Equipo equipo) {
        return gimnasioService.agregarEquipo(equipo);
    }

    @GetMapping("")
    public List<Equipo> obtenerTodosEquipos() {
        return gimnasioService.obtenerTodosEquipos();
    }
}
