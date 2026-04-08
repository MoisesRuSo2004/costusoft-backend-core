package com.costusoft.inventory_system.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 *
 * Principios aplicados:
 * - Stateless: sin sesiones HTTP, cada request se autentica con JWT
 * - CSRF deshabilitado: innecesario en APIs stateless
 * - CORS configurado en CorsConfig (separado de responsabilidades)
 * - @EnableMethodSecurity: activa @PreAuthorize en controllers y services
 * - Rutas públicas mínimas: solo /api/auth/** y Swagger
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPoint authEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    // ── Rutas públicas ───────────────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Sin sesiones — 100% stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF innecesario en APIs REST stateless
                .csrf(AbstractHttpConfigurer::disable)

                // CORS delegado a CorsConfig
                .cors(cors -> {
                })

                // Manejo de errores de autenticación/autorización con JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // Reglas de acceso
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // Solo ADMIN puede gestionar usuarios
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // Solo ADMIN puede gestionar proveedores
                        .requestMatchers("/api/proveedores/**").hasRole("ADMIN")

                        // Inventario: BODEGA puede ver pero no crear/editar
                        // (los endpoints individuales con @PreAuthorize granular aplican la restricción exacta)
                        .requestMatchers("/api/insumos/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/entradas/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/salidas/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/colegios/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/uniformes/**").hasAnyRole("ADMIN", "USER", "BODEGA")

                        // Dashboard, reportes y calculadora accesibles para todos los roles
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/reporte/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/calculadora/**").hasAnyRole("ADMIN", "USER", "BODEGA")
                        .requestMatchers("/api/prediccion/**").hasAnyRole("ADMIN", "USER", "BODEGA")

                        // Pedidos: todos los roles pueden ver; USER/ADMIN crean/editan;
                        // BODEGA gestiona la producción y entrega (control granular en @PreAuthorize)
                        .requestMatchers("/api/pedidos/**").hasAnyRole("ADMIN", "USER", "BODEGA")

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated())

                // Inyectar el filtro JWT antes del filtro de usuario/contraseña estándar
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt con strength 12 — balance seguridad/rendimiento para producción
        return new BCryptPasswordEncoder(12);
    }
}
