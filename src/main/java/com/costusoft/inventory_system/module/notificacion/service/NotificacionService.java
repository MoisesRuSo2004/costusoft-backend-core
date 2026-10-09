package com.costusoft.inventory_system.module.notificacion.service;

import com.costusoft.inventory_system.module.notificacion.dto.NotificacionDTO;

/**
 * Registro de dispositivos y avisos push a la app movil.
 */
public interface NotificacionService {

    /** Registra el token del dispositivo para el usuario (o lo reasigna si ya existia). */
    void registrarDispositivo(NotificacionDTO.DispositivoRequest request, String username);

    /** Elimina el token si pertenece al usuario (al cerrar sesion en la app). */
    void eliminarDispositivo(String token, String username);

    /** Envia una notificacion de prueba a los dispositivos del usuario. Devuelve cuantos la recibieron. */
    int enviarPrueba(String username);

    /**
     * Reenvia el aviso de un pedido que ya esta LISTO_PARA_ENTREGA, con el mismo
     * mensaje del flujo real (solo perfil dev: probar sin cambiar el estado de pedidos).
     */
    int reenviarPedidoListo(Long pedidoId);
}
