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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gimnasio/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    // 202 Accepted: el pago queda PENDIENTE y se procesa de forma asíncrona
    @PostMapping("")
    public ResponseEntity<Pago> solicitarPago(@RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(pagoService.solicitarPago(request));
    }

    @GetMapping("")
    public List<Pago> obtenerTodosPagos() {
        return pagoService.obtenerTodosPagos();
    }

    @GetMapping("/fallidos")
    public List<Pago> obtenerPagosFallidos() {
        return pagoService.obtenerPagosFallidos();
    }
}
