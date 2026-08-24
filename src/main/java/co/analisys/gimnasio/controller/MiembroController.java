package co.analisys.gimnasio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.service.GimnasioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gimnasio/miembros")
@RequiredArgsConstructor
public class MiembroController {

    private final GimnasioService gimnasioService;

    @PostMapping("")
    public Miembro registrarMiembro(@RequestBody Miembro miembro) {
        return gimnasioService.registrarMiembro(miembro);
    }

    @GetMapping("")
    public List<Miembro> obtenerTodosMiembros() {
        return gimnasioService.obtenerTodosMiembros();
    }
}
