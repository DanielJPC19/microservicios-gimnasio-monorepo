package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/gimnasio/entrenadores")
@RequiredArgsConstructor
@Tag(name = "Entrenadores", description = "Proxy de entrenadores a traves del API Gateway")
public class EntrenadorGatewayController {

    @Value("${entrenador.service.url}")
    private String entrenadorServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Listar entrenadores", description = "Obtiene la lista de todos los entrenadores")
    @ApiResponse(responseCode = "200", description = "Lista de entrenadores")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerTodosEntrenadores() {
        String respuesta = restClient.get()
                .uri(entrenadorServiceUrl + "/api/gimnasio/entrenadores")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear entrenador", description = "Agrega un nuevo entrenador. Solo administradores.")
    @ApiResponse(responseCode = "200", description = "Entrenador creado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<String> agregarEntrenador(@RequestBody String entrenador) {
        String respuesta = restClient.post()
                .uri(entrenadorServiceUrl + "/api/gimnasio/entrenadores")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(entrenador)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Obtener entrenador por ID", description = "Obtiene un entrenador especifico")
    @ApiResponse(responseCode = "200", description = "Entrenador encontrado")
    @ApiResponse(responseCode = "404", description = "Entrenador no encontrado")
    public ResponseEntity<String> obtenerEntrenadorPorId(@PathVariable Long id) {
        String respuesta = restClient.get()
                .uri(entrenadorServiceUrl + "/api/gimnasio/entrenadores/" + id)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
