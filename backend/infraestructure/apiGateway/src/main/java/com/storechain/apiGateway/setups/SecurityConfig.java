package com.storechain.apiGateway.setups;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                // Preflight CORS siempre pasa
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Alertas de bajo stock (generadas vía Kafka): solo con sesión. Va antes que la regla pública del catálogo
                .pathMatchers(HttpMethod.GET, "/api/inventory/v1/alerts/**").authenticated()
                // Catálogo de productos: público (clientes sin login)
                .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()
                // Checkout via BFF: público (compra sin login)
                .pathMatchers("/api/bff/**").permitAll()
                // Order POST desde BFF interno: no lleva JWT de usuario
                .pathMatchers(HttpMethod.POST, "/api/order/**").permitAll()
                // Servicios técnicos: catálogo, contratar y seguir una solicitud son públicos (como el checkout)
                .pathMatchers(HttpMethod.GET, "/api/technician/v1").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/technician/v1/hire").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/technician/v1/track/**").permitAll()
                // Todo lo demás (solicitudes, pagos, notificaciones) requiere JWT válido de Azure Entra ID
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwkSetUri(
                    "https://login.microsoftonline.com/49d2b527-d999-453e-9ca1-ae9dd9284fc4/discovery/v2.0/keys"
                ))
            );
        return http.build();
    }
}
