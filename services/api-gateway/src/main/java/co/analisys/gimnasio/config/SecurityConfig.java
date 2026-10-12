package co.analisys.gimnasio.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Configuracion reactiva de seguridad para Spring Cloud Gateway.
 * Valida tokens JWT emitidos por Keycloak y extrae los roles desde realm_access.roles.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Rutas publicas y de monitoreo
                .pathMatchers("/actuator/**", "/api/gimnasio").permitAll()

                // Reglas especificas por rol y metodo HTTP en el perimetro
                .pathMatchers(HttpMethod.POST, "/api/gimnasio/entrenadores/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/api/gimnasio/pagos").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/gimnasio/equipos/**").hasAnyRole("ADMIN", "TRAINER")
                .pathMatchers(HttpMethod.PUT, "/api/gimnasio/clases/**").hasAnyRole("ADMIN", "TRAINER")
                .pathMatchers(HttpMethod.GET, "/api/gimnasio/monitoreo/**").hasAnyRole("ADMIN", "TRAINER")
                .pathMatchers(HttpMethod.POST, "/api/gimnasio/miembros").hasAnyRole("ADMIN", "MEMBER")
                .pathMatchers(HttpMethod.GET, "/api/gimnasio/miembros").hasAnyRole("ADMIN", "TRAINER")
                .pathMatchers(HttpMethod.GET, "/api/gimnasio/miembros/*").hasAnyRole("ADMIN", "TRAINER", "MEMBER")
                .pathMatchers(HttpMethod.POST, "/api/gimnasio/clases").hasAnyRole("ADMIN", "TRAINER")
                .pathMatchers(HttpMethod.GET, "/api/gimnasio/clases/**").hasAnyRole("ADMIN", "TRAINER", "MEMBER")
                .pathMatchers("/api/gimnasio/agregacion/**").hasAnyRole("ADMIN", "TRAINER", "MEMBER")

                // Cualquier otra peticion hacia la API del gimnasio debe estar autenticada
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter()))
            )
            .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> rawRoles)) {
                return Collections.emptyList();
            }
            return rawRoles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }
}
