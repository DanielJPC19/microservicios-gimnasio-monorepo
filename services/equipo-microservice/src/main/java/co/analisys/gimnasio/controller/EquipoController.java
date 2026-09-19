package co.analisys.gimnasio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import co.analisys.gimnasio.dto.ReporteAveriaRequest;

@RestController
@RequestMapping("/api/gimnasio/equipos")
@RequiredArgsConstructor
@Tag(name = "Equipos", description = "Gestion de equipos e inventario del gimnasio")
public class EquipoController {

    private final EquipoService equipoService;

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agregar equipo", description = "Registra un nuevo equipo en el inventario. Solo administradores.")
    @ApiResponse(responseCode = "200", description = "Equipo agregado exitosamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN")
    public Equipo agregarEquipo(@RequestBody Equipo equipo) {
        return equipoService.agregarEquipo(equipo);
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Listar equipos", description = "Obtiene la lista de todos los equipos del gimnasio")
    @ApiResponse(responseCode = "200", description = "Lista de equipos obtenida exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido o ausente")
    public List<Equipo> obtenerTodosEquipos() {
        return equipoService.obtenerTodosEquipos();
    }

    @PostMapping("/{id}/reportar-averia")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Reportar averia", description = "Reporta una averia en un equipo. Publica evento via RabbitMQ.")
    @ApiResponse(responseCode = "200", description = "Averia reportada y evento publicado")
    @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    public Equipo reportarAveria(@PathVariable Long id, @RequestBody ReporteAveriaRequest reporte) {
        return equipoService.reportarAveria(id, reporte);
    }
}
