package co.analisys.gimnasio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.analisys.gimnasio.dto.PagoRequest;
import co.analisys.gimnasio.model.Pago;
import co.analisys.gimnasio.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/gimnasio/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Procesamiento de pagos del gimnasio (con Dead Letter Queue)")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Solicitar pago", description = "Registra un pago y lo procesa de forma asincrona (202 Accepted)")
    @ApiResponse(responseCode = "202", description = "Pago recibido y en proceso de validacion")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public ResponseEntity<Pago> solicitarPago(@RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pagoService.solicitarPago(request));
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar pagos", description = "Obtiene todos los pagos registrados con su estado e intentos")
    @ApiResponse(responseCode = "200", description = "Lista de pagos obtenida exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido o ausente")
    public List<Pago> obtenerTodosPagos() {
        return pagoService.obtenerTodosPagos();
    }

    @GetMapping("/fallidos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar pagos fallidos", description = "Obtiene los pagos que fallaron definitivamente (Dead Letter Queue)")
    @ApiResponse(responseCode = "200", description = "Lista de pagos fallidos obtenida")
    @ApiResponse(responseCode = "401", description = "Token JWT no valido o ausente")
    public List<Pago> obtenerPagosFallidos() {
        return pagoService.obtenerPagosFallidos();
    }
}
