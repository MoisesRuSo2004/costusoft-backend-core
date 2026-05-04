package com.costusoft.inventory_system.exception;

public class InsumoConMovimientosException extends RuntimeException {

    public InsumoConMovimientosException(String nombreInsumo) {
        super("El insumo \"" + nombreInsumo + "\" tiene movimientos registrados y no puede eliminarse. Puedes inhabilitarlo en su lugar.");
    }
}
