package com.costusoft.inventory_system.exception;

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

    /** Mensaje estandar para un servicio de analisis que todavia arranca. */
    public static ServicioNoDisponibleException de(String servicio) {
        return new ServicioNoDisponibleException(
                "El " + servicio + " se está iniciando. Intenta de nuevo en unos segundos.");
    }
}
