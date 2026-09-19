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
@RequestMapping("/api/gimnasio/miembros")
@RequiredArgsConstructor
@Tag(name = "Miembros", description = "Proxy de miembros a traves del API Gateway")
public class MiembroGatewayController {

    @Value("${miembro.service.url}")
    private String miembroServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @Operation(summary = "Listar miembros", description = "Obtiene la lista de todos los miembros")
    @ApiResponse(responseCode = "200", description = "Lista de miembros")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerTodosMiembros() {
        String respuesta = restClient.get()
                .uri(miembroServiceUrl + "/api/gimnasio/miembros")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Registrar miembro", description = "Registra un nuevo miembro en el gimnasio")
    @ApiResponse(responseCode = "200", description = "Miembro registrado")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<String> registrarMiembro(@RequestBody String miembro) {
        String respuesta = restClient.post()
                .uri(miembroServiceUrl + "/api/gimnasio/miembros")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(miembro)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
