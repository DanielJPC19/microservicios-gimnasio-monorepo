package co.analisys.gimnasio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.dto.DatosEntrenamiento;
import co.analisys.gimnasio.dto.RegistroEntrenamientoRequest;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.service.MiembroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/gimnasio/miembros")
@RequiredArgsConstructor
@Tag(name = "Miembros", description = "Gestion de miembros/socios del gimnasio")
public class MiembroController {

    private final MiembroService miembroService;

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Registrar miembro", description = "Registra un nuevo miembro/socio en el gimnasio")
    @ApiResponse(responseCode = "200", description = "Miembro registrado exitosamente")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public Miembro registrarMiembro(@RequestBody Miembro miembro) {
        return miembroService.registrarMiembro(miembro);
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Listar miembros", description = "Obtiene la lista de todos los miembros registrados")
    @ApiResponse(responseCode = "200", description = "Lista de miembros obtenida exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido o ausente")
    public List<Miembro> obtenerTodosMiembros() {
        return miembroService.obtenerTodosMiembros();
    }

    @PostMapping("/{id}/entrenamientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Registrar entrenamiento", description = "Publica una sesion de entrenamiento del miembro en el topic datos-entrenamiento")
    @ApiResponse(responseCode = "200", description = "Entrenamiento registrado")
    @ApiResponse(responseCode = "400", description = "Datos de entrenamiento invalidos")
    @ApiResponse(responseCode = "404", description = "Miembro no encontrado")
    @ApiResponse(responseCode = "503", description = "Kafka no disponible")
    public DatosEntrenamiento registrarEntrenamiento(@PathVariable Long id, @RequestBody RegistroEntrenamientoRequest request) {
        return miembroService.registrarEntrenamiento(id, request);
    }
}
