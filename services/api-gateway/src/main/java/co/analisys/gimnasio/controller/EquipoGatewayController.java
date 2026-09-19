package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/gimnasio/equipos")
@RequiredArgsConstructor
@Tag(name = "Equipos", description = "Proxy de equipos a traves del API Gateway")
public class EquipoGatewayController {

    @Value("${equipo.service.url}")
    private String equipoServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Listar equipos", description = "Obtiene la lista de todos los equipos")
    @ApiResponse(responseCode = "200", description = "Lista de equipos")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerTodosEquipos() {
        String respuesta = restClient.get()
                .uri(equipoServiceUrl + "/api/gimnasio/equipos")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agregar equipo", description = "Registra un nuevo equipo en el inventario")
    @ApiResponse(responseCode = "200", description = "Equipo agregado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<String> agregarEquipo(@RequestBody String equipo) {
        String respuesta = restClient.post()
                .uri(equipoServiceUrl + "/api/gimnasio/equipos")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(equipo)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/reportar-averia")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Reportar averia", description = "Reporta una averia y publica evento via RabbitMQ")
    @ApiResponse(responseCode = "200", description = "Averia reportada")
    @ApiResponse(responseCode = "404", description = "Equipo no encontrado")
    public ResponseEntity<String> reportarAveria(@PathVariable Long id, @RequestBody String reporte) {
        String respuesta = restClient.post()
                .uri(equipoServiceUrl + "/api/gimnasio/equipos/" + id + "/reportar-averia")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(reporte)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }
}
