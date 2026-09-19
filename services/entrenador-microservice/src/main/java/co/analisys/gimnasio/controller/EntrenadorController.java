package co.analisys.gimnasio.controller;

import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.service.EntrenadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gimnasio/entrenadores")
@RequiredArgsConstructor
@Tag(name = "Entrenadores", description = "Gestion de entrenadores del gimnasio")
public class EntrenadorController {
    private final EntrenadorService entrenadorService;

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear entrenador", description = "Agrega un nuevo entrenador al sistema. Solo administradores.")
    @ApiResponse(responseCode = "200", description = "Entrenador creado exitosamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    public Entrenador agregarEntrenador(@RequestBody Entrenador entrenador) {
        return entrenadorService.agregarEntrenador(entrenador);
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Listar entrenadores", description = "Obtiene la lista de todos los entrenadores registrados")
    @ApiResponse(responseCode = "200", description = "Lista de entrenadores obtenida exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido o ausente")
    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorService.obtenerTodosEntrenadores();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Obtener entrenador por ID", description = "Obtiene un entrenador especifico por su ID")
    @ApiResponse(responseCode = "200", description = "Entrenador encontrado")
    @ApiResponse(responseCode = "404", description = "Entrenador no encontrado")
    public Entrenador obtenerEntrenadorPorId(@PathVariable Long id) {
        return entrenadorService.obtenerEntrenadorPorId(id);
    }
}
