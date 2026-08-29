package co.analisys.gimnasio.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.dto.ClaseResponse;
import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.service.ClaseService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/gimnasio/clases")
@RequiredArgsConstructor
public class ClaseController {

    private final ClaseService claseService;

    @PostMapping("")
    public Clase programarClase(@RequestBody Clase clase) {
        return claseService.programarClase(clase);
    }

    @GetMapping("")
    public List<ClaseResponse> obtenerTodasClases() {
        return claseService.obtenerTodasClases();
    }
}
