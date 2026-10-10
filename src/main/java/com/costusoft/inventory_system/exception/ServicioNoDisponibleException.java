package com.costusoft.inventory_system.exception;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Set;

/**
 * Un microservicio de apoyo (prediccion u optimizacion) no respondio.
 *
 * En produccion suele ser temporal: Render duerme los servicios sin uso y
 * tardan 30-60 s en arrancar. Mapea a HTTP 503 con Retry-After para que los
 * clientes reintenten en lugar de mostrar un error definitivo.
 *
 * El mensaje es para el usuario final: nunca debe incluir URLs, puertos ni
 * variables de entorno (esos detalles van solo al log).
 */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String message) {
        super(message);
    }

    /** Codigos que Render (o el servicio) devuelve mientras arranca o esta saturado. */
    private static final Set<Integer> CODIGOS_TEMPORALES = Set.of(429, 502, 503, 504);

    /**
     * true si el fallo se arregla esperando: sin conexion, tiempo agotado o
     * 429/502/503/504. Un 4xx distinto es un error real de la peticion.
     */
    public static boolean esTemporal(Exception e) {
        if (e instanceof ResourceAccessException) return true;
        return e instanceof RestClientResponseException r
                && CODIGOS_TEMPORALES.contains(r.getStatusCode().value());
    }

    /** Mensaje estandar para un servicio de analisis que todavia arranca. */
    public static ServicioNoDisponibleException de(String servicio) {
        return new ServicioNoDisponibleException(
                "El " + servicio + " se está iniciando. Intenta de nuevo en unos segundos.");
    }
}
