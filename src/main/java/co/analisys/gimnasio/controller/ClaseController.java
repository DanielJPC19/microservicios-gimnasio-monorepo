package co.analisys.gimnasio.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.service.GimnasioService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/gimnasio/clases")
@RequiredArgsConstructor
public class ClaseController {

    private final GimnasioService gimnasioService;

    @PostMapping("")
    public Clase programarClase(@RequestBody Clase clase) {
        return gimnasioService.programarClase(clase);
    }


    @GetMapping("")
    public List<Clase> obtenerTodasClases() {
        return gimnasioService.obtenerTodasClases();
    }
}
