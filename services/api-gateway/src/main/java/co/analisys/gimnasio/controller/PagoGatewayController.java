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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/gimnasio/pagos")
@RequiredArgsConstructor
public class PagoGatewayController {

    @Value("${pago.service.url}")
    private String pagoServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    public ResponseEntity<String> obtenerTodosPagos() {
        String respuesta = restClient.get()
                .uri(pagoServiceUrl + "/api/gimnasio/pagos")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/fallidos")
    public ResponseEntity<String> obtenerPagosFallidos() {
        String respuesta = restClient.get()
                .uri(pagoServiceUrl + "/api/gimnasio/pagos/fallidos")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
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
