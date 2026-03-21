package com.costusoft.inventory_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lanzada cuando un recurso solicitado no existe en la base de datos.
 * Mapea automáticamente a HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s con id [%d] no encontrado", resource, id));
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("%s con %s [%s] no encontrado", resource, field, value));
    }
}
