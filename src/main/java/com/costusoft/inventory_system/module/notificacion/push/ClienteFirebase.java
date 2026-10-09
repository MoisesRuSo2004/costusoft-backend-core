package com.costusoft.inventory_system.module.notificacion.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Inicializa Firebase Admin con la cuenta de servicio del proyecto.
 *
 * La credencial es SECRETA y nunca va en el repositorio. Se lee de:
 *   - FIREBASE_CREDENTIALS_JSON: el contenido completo del JSON (produccion / Render), o
 *   - FIREBASE_CREDENTIALS_PATH: la ruta al archivo JSON (desarrollo local).
 *
 * Sin credencial el backend arranca igual: las notificaciones quedan desactivadas
 * y solo se registra una advertencia.
 */
@Slf4j
@Component
public class ClienteFirebase {

    private final String credencialesJson;
    private final String credencialesRuta;

    private FirebaseMessaging mensajeria;

    public ClienteFirebase(
            @Value("${app.firebase.credenciales-json:}") String credencialesJson,
            @Value("${app.firebase.credenciales-ruta:}") String credencialesRuta) {
        this.credencialesJson = credencialesJson;
        this.credencialesRuta = credencialesRuta;
    }

    @PostConstruct
    void inicializar() {
        if (credencialesJson.isBlank() && credencialesRuta.isBlank()) {
            log.warn("Firebase sin credencial (FIREBASE_CREDENTIALS_JSON / FIREBASE_CREDENTIALS_PATH): "
                    + "las notificaciones push estan desactivadas.");
            return;
        }
        try (InputStream credencial = abrirCredencial()) {
            GoogleCredentials credenciales = GoogleCredentials.fromStream(credencial);
            FirebaseOptions.Builder constructor = FirebaseOptions.builder().setCredentials(credenciales);
            if (credenciales instanceof ServiceAccountCredentials cuenta) {
                constructor.setProjectId(cuenta.getProjectId());
            }
            FirebaseOptions opciones = constructor.build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(opciones)
                    : FirebaseApp.getInstance();
            mensajeria = FirebaseMessaging.getInstance(app);
            log.info("Firebase inicializado — notificaciones push activas (proyecto: {})",
                    app.getOptions().getProjectId());
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo inicializar Firebase; las notificaciones push quedan desactivadas: {}",
                    e.getMessage());
        }
    }

    private InputStream abrirCredencial() throws IOException {
        return credencialesJson.isBlank()
                ? new FileInputStream(credencialesRuta)
                : new ByteArrayInputStream(credencialesJson.getBytes(StandardCharsets.UTF_8));
    }

    /** Vacio si Firebase no esta configurado. */
    public Optional<FirebaseMessaging> mensajeria() {
        return Optional.ofNullable(mensajeria);
    }
}
