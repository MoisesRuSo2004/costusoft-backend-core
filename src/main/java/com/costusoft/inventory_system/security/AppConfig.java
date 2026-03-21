package com.costusoft.inventory_system.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración general de la aplicación:
 * - JPA Auditing (createdAt / updatedAt automáticos)
 * - RestTemplate para llamadas HTTP externas
 * - OpenAPI / Swagger UI con soporte para JWT en el header
 */
@Configuration
@EnableJpaAuditing
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Configura Swagger UI para incluir el botón "Authorize"
     * que inyecta el token JWT como Bearer en todos los requests.
     *
     * Acceso: http://localhost:8080/swagger-ui.html
     */
    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Inventario API")
                        .description("Sistema de Gestión y Control de Inventario — REST API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Inventario")
                                .email("soporte@inventario.com")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa el token JWT obtenido desde /api/auth/login")));
    }
}