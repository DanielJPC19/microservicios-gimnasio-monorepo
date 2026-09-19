package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/gimnasio/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Proxy de pagos a traves del API Gateway")
public class PagoGatewayController {

    @Value("${pago.service.url}")
    private String pagoServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar pagos", description = "Obtiene todos los pagos registrados")
    @ApiResponse(responseCode = "200", description = "Lista de pagos")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerTodosPagos() {
        String respuesta = restClient.get()
                .uri(pagoServiceUrl + "/api/gimnasio/pagos")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/fallidos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar pagos fallidos", description = "Obtiene los pagos que fallaron (DLQ)")
    @ApiResponse(responseCode = "200", description = "Lista de pagos fallidos")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido")
    public ResponseEntity<String> obtenerPagosFallidos() {
        String respuesta = restClient.get()
                .uri(pagoServiceUrl + "/api/gimnasio/pagos/fallidos")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Solicitar pago", description = "Registra un pago para procesamiento asincrono (202 Accepted)")
    @ApiResponse(responseCode = "202", description = "Pago recibido y en proceso")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<String> solicitarPago(@RequestBody String pago) {
        String respuesta = restClient.post()
                .uri(pagoServiceUrl + "/api/gimnasio/pagos")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(pago)
                .retrieve()
                .body(String.class);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respuesta);
    }
}
