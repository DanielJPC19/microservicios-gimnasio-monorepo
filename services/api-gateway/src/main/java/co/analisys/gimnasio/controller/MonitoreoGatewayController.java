package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

// El stream SSE (/ocupacion/stream) se consume directamente en monitoreo-service (8087):
// este proxy lee la respuesta completa y no puede reenviar una conexión abierta.
@RestController
@RequestMapping("/api/gimnasio/monitoreo")
@RequiredArgsConstructor
@Tag(name = "Monitoreo", description = "Proxy de monitoreo (Kafka) a traves del API Gateway")
public class MonitoreoGatewayController {

    @Value("${monitoreo.service.url}")
    private String monitoreoServiceUrl;

    private final RestClient restClient;

    @GetMapping("/ocupacion")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Ocupacion actual", description = "Estado actual de ocupacion de cada clase")
    @ApiResponse(responseCode = "200", description = "Ocupacion por clase")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerOcupaciones() {
        String respuesta = restClient.get()
                .uri(monitoreoServiceUrl + "/api/gimnasio/monitoreo/ocupacion")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/entrenamiento/{miembroId}/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Resumen semanal de entrenamiento", description = "Ventanas de 7 dias calculadas por Kafka Streams")
    @ApiResponse(responseCode = "200", description = "Resumenes por ventana")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerResumenEntrenamiento(
            @PathVariable Long miembroId,
            @RequestParam(defaultValue = "4") int semanas) {
        String respuesta = restClient.get()
                .uri(monitoreoServiceUrl + "/api/gimnasio/monitoreo/entrenamiento/" + miembroId + "/resumen?semanas=" + semanas)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/recuperacion/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estado de recuperacion", description = "Checkpoints por particion y eventos procesados")
    @ApiResponse(responseCode = "200", description = "Checkpoints y conteo de eventos")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<String> obtenerEstadoRecuperacion() {
        String respuesta = restClient.get()
                .uri(monitoreoServiceUrl + "/api/gimnasio/monitoreo/recuperacion/estado")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }
}
