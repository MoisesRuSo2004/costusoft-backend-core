package com.costusoft.inventory_system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lanzada cuando se intenta registrar una salida que supera el stock
 * disponible.
 * Mapea a HTTP 422 Unprocessable Entity — la request es válida pero no
 * procesable.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class StockInsuficienteException extends RuntimeException {

    private final String nombreInsumo;
    private final int stockActual;
    private final int cantidadSolicitada;

    public StockInsuficienteException(String nombreInsumo, int stockActual, int cantidadSolicitada) {
        super(String.format(
                "Stock insuficiente para '%s'. Disponible: %d, Solicitado: %d",
                nombreInsumo, stockActual, cantidadSolicitada));
        this.nombreInsumo = nombreInsumo;
        this.stockActual = stockActual;
        this.cantidadSolicitada = cantidadSolicitada;
    }

    public String getNombreInsumo() {
        return nombreInsumo;
    }

    public int getStockActual() {
        return stockActual;
    }

    public int getCantidadSolicitada() {
        return cantidadSolicitada;
    }
}
