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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/gimnasio/clases")
@RequiredArgsConstructor
@Tag(name = "Clases", description = "Proxy de clases a traves del API Gateway")
public class ClaseGatewayController {

    @Value("${clase.service.url}")
    private String claseServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Listar clases", description = "Obtiene todas las clases programadas")
    @ApiResponse(responseCode = "200", description = "Lista de clases")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerTodasClases() {
        String respuesta = restClient.get()
                .uri(claseServiceUrl + "/api/gimnasio/clases")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Programar clase", description = "Crea una nueva clase en el horario")
    @ApiResponse(responseCode = "200", description = "Clase programada")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<String> programarClase(@RequestBody String clase) {
        String respuesta = restClient.post()
                .uri(claseServiceUrl + "/api/gimnasio/clases")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(clase)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
