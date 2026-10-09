package com.costusoft.inventory_system.module.notificacion.push;

import com.costusoft.inventory_system.repo.DispositivoNotificacionRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Envia una notificacion a varios dispositivos y limpia los tokens que Firebase
 * reporta como vencidos (app desinstalada, sesion cerrada, etc.).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioPush {

    /** Lote maximo por llamada a Firebase. */
    private static final int LOTE = 500;

    /** Errores que significan "este token ya no sirve". */
    private static final Set<MessagingErrorCode> TOKEN_INVALIDO =
            EnumSet.of(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT);

    private final ClienteFirebase clienteFirebase;
    private final DispositivoNotificacionRepository dispositivoRepository;

    /**
     * @param datos pares clave/valor que la app usa al tocar la notificacion
     *              (p. ej. tipo y pedidoId para abrir el seguimiento).
     * @return cantidad de dispositivos que recibieron el mensaje.
     */
    public int enviar(List<String> tokens, String titulo, String cuerpo, Map<String, String> datos) {
        if (tokens.isEmpty()) return 0;
        FirebaseMessaging mensajeria = clienteFirebase.mensajeria().orElse(null);
        if (mensajeria == null) {
            log.warn("Push omitido (Firebase no configurado): '{}' a {} dispositivo(s)", titulo, tokens.size());
            return 0;
        }

        int entregados = 0;
        List<String> invalidos = new ArrayList<>();
        for (int desde = 0; desde < tokens.size(); desde += LOTE) {
            List<String> lote = tokens.subList(desde, Math.min(desde + LOTE, tokens.size()));
            MulticastMessage mensaje = MulticastMessage.builder()
                    .addAllTokens(lote)
                    .setNotification(Notification.builder().setTitle(titulo).setBody(cuerpo).build())
                    .putAllData(datos)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setColor("#0891B2")
                                    .build())
                            .build())
                    .build();
            try {
                BatchResponse respuesta = mensajeria.sendEachForMulticast(mensaje);
                entregados += respuesta.getSuccessCount();
                List<SendResponse> resultados = respuesta.getResponses();
                for (int i = 0; i < resultados.size(); i++) {
                    FirebaseMessagingException error = resultados.get(i).getException();
                    if (error != null && TOKEN_INVALIDO.contains(error.getMessagingErrorCode())) {
                        invalidos.add(lote.get(i));
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("Fallo el envio push '{}': {}", titulo, e.getMessage());
            }
        }

        if (!invalidos.isEmpty()) {
            int eliminados = dispositivoRepository.eliminarTokens(invalidos);
            log.info("Tokens push vencidos eliminados: {}", eliminados);
        }
        log.info("Push '{}' entregado a {}/{} dispositivo(s)", titulo, entregados, tokens.size());
        return entregados;
    }
}
