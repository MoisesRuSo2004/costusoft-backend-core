package com.costusoft.inventory_system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración CORS separada de SecurityConfig por claridad.
 *
 * En desarrollo se permiten todos los orígenes para facilitar
 * el trabajo con Flutter Web y Postman.
 * En producción, origins se limita a los dominios reales del frontend.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (desde application.yml — diferente por perfil)
        config.setAllowedOriginPatterns(allowedOrigins);

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers permitidos en el request
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"));

        // Headers expuestos al cliente en la response
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        // Permite enviar cookies/credenciales (necesario si usas refresh tokens en
        // cookie)
        config.setAllowCredentials(true);

        // Cache del preflight por 1 hora
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}