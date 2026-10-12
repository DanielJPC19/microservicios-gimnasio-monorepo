package co.analisys.gimnasio.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion programatica de enrutamiento para cumplir con el Requisito 2 del documento.
 * Permite enrutar peticiones dinamicamente hacia el balanceador de carga lb://equipo-service.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("ruta-equipos-programatica", r -> r
                .path("/api/gimnasio/equipos/**")
                .uri("lb://equipo-service"))
            .build();
    }
}
