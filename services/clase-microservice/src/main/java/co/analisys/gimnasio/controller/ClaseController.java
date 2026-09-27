package co.analisys.gimnasio.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.dto.CambioHorarioClaseRequest;
import co.analisys.gimnasio.dto.ClaseRequest;
import co.analisys.gimnasio.dto.ClaseResponse;
import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.service.ClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/gimnasio/clases")
@RequiredArgsConstructor
@Tag(name = "Clases", description = "Programacion de clases del gimnasio")
public class ClaseController {

    private final ClaseService claseService;

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Programar clase", description = "Crea una nueva clase en el horario del gimnasio validando que el entrenador exista")
    @ApiResponse(responseCode = "200", description = "Clase programada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de la clase invalidos")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    @ApiResponse(responseCode = "404", description = "El entrenador indicado no existe")
    public Clase programarClase(@Valid @RequestBody ClaseRequest request) {
        return claseService.programarClase(request.toEntity());
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Listar clases", description = "Obtiene la lista de todas las clases programadas con datos del entrenador")
    @ApiResponse(responseCode = "200", description = "Lista de clases obtenida exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido o ausente")
    public List<ClaseResponse> obtenerTodasClases() {
        return claseService.obtenerTodasClases();
    }

    @PutMapping("/{id}/horario")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Cambiar horario de clase", description = "Actualiza el horario de una clase y difunde el evento via RabbitMQ FanoutExchange (app movil, emailing y auditoria)")
    @ApiResponse(responseCode = "200", description = "Horario actualizado y evento difundido")
    @ApiResponse(responseCode = "400", description = "Horario invalido")
    @ApiResponse(responseCode = "404", description = "Clase no encontrada")
    public Clase cambiarHorario(@PathVariable Long id, @Valid @RequestBody CambioHorarioClaseRequest request) {
        return claseService.cambiarHorario(id, request);
    }

    @PostMapping("/{id}/ingreso")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Registrar ingreso a clase", description = "Incrementa la ocupacion de la clase y publica la actualizacion en el topic ocupacion-clases")
    @ApiResponse(responseCode = "200", description = "Ingreso registrado")
    @ApiResponse(responseCode = "404", description = "Clase no encontrada")
    @ApiResponse(responseCode = "409", description = "La clase ya esta llena")
    public Clase registrarIngreso(@PathVariable Long id) {
        return claseService.registrarIngreso(id);
    }

    @PostMapping("/{id}/salida")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Registrar salida de clase", description = "Decrementa la ocupacion de la clase y publica la actualizacion en el topic ocupacion-clases")
    @ApiResponse(responseCode = "200", description = "Salida registrada")
    @ApiResponse(responseCode = "404", description = "Clase no encontrada")
    @ApiResponse(responseCode = "409", description = "La clase no tiene asistentes")
    public Clase registrarSalida(@PathVariable Long id) {
        return claseService.registrarSalida(id);
    }
}
