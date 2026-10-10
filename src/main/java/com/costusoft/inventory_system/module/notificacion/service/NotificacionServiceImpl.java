package com.costusoft.inventory_system.module.notificacion.service;

import com.costusoft.inventory_system.entity.DispositivoNotificacion;
import com.costusoft.inventory_system.entity.Pedido;
import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.notificacion.dto.NotificacionDTO;
import com.costusoft.inventory_system.module.notificacion.evento.MovimientoPendienteEvent;
import com.costusoft.inventory_system.module.notificacion.evento.PedidoListoEvent;
import com.costusoft.inventory_system.module.notificacion.push.EnvioPush;
import com.costusoft.inventory_system.repo.DispositivoNotificacionRepository;
import com.costusoft.inventory_system.repo.PedidoRepository;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final DispositivoNotificacionRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnvioPush envioPush;
    private final PedidoRepository pedidoRepository;

    /**
     * Los avisos se envian en segundo plano: la respuesta a quien marco el pedido
     * no espera a Firebase. Un solo hilo basta para el volumen del taller.
     */
    private final ExecutorService hiloEnvios = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "push-notificaciones");
        hilo.setDaemon(true);
        return hilo;
    });

    @PreDestroy
    void detener() {
        hiloEnvios.shutdown();
    }

    @Override
    @Transactional
    public void registrarDispositivo(NotificacionDTO.DispositivoRequest request, String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
        String token = request.getToken().trim();

        DispositivoNotificacion dispositivo = dispositivoRepository.findByToken(token)
                .orElseGet(() -> DispositivoNotificacion.builder().token(token).build());
        dispositivo.setUsuario(usuario);
        dispositivo.setPlataforma(request.getPlataforma());
        dispositivoRepository.save(dispositivo);
        log.info("Dispositivo push registrado — usuario:'{}' | plataforma:{}", username, request.getPlataforma());
    }

    @Override
    @Transactional
    public void eliminarDispositivo(String token, String username) {
        dispositivoRepository.findByToken(token.trim())
                .filter(d -> d.getUsuario().getUsername().equals(username))
                .ifPresent(d -> {
                    dispositivoRepository.delete(d);
                    log.info("Dispositivo push eliminado — usuario:'{}'", username);
                });
    }

    @Override
    public int enviarPrueba(String username) {
        return envioPush.enviar(
                dispositivoRepository.tokensDeUsuario(username),
                "Notificaciones activas",
                "Así te llegarán los avisos de CostuSoft en este dispositivo.",
                Map.of("tipo", "PRUEBA"));
    }

    /**
     * Avisa a los coordinadores del colegio que su pedido esta listo.
     * Corre despues del commit: si la transaccion falla, no se avisa nada.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alPedidoListo(PedidoListoEvent evento) {
        hiloEnvios.execute(() -> {
            try {
                avisarPedidoListo(evento);
            } catch (RuntimeException e) {
                log.error("No se pudo avisar el pedido listo {}: {}", evento.numeroPedido(), e.getMessage());
            }
        });
    }

    /**
     * Avisa a Bodega que hay una entrada o salida por confirmar.
     * Corre despues del commit: si el registro falla, no se avisa nada.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alMovimientoPendiente(MovimientoPendienteEvent evento) {
        hiloEnvios.execute(() -> {
            try {
                boolean entrada = "ENTRADA".equals(evento.tipo());
                String insumos = evento.renglones() == 1 ? "1 insumo" : evento.renglones() + " insumos";
                String detalle = evento.tercero() != null
                        ? (entrada ? " de " : " para ") + evento.tercero()
                        : "";
                envioPush.enviar(
                        dispositivoRepository.tokensDeRol(Usuario.Rol.BODEGA),
                        entrada ? "Nueva entrada por confirmar" : "Nueva salida por confirmar",
                        (entrada ? "Entrada #" : "Salida #") + evento.movimientoId() + detalle + " · " + insumos + ".",
                        Map.of("tipo", "MOVIMIENTO_PENDIENTE",
                                "movimiento", evento.tipo(),
                                "movimientoId", String.valueOf(evento.movimientoId())));
            } catch (RuntimeException e) {
                log.error("No se pudo avisar a Bodega del movimiento {} #{}: {}",
                        evento.tipo(), evento.movimientoId(), e.getMessage());
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public int reenviarPedidoListo(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));
        if (!pedido.esListoParaEntrega()) {
            throw new BusinessException("El pedido no esta LISTO_PARA_ENTREGA. Estado: " + pedido.getEstado());
        }
        return avisarPedidoListo(new PedidoListoEvent(pedido.getId(), pedido.getNumeroPedido(),
                pedido.getColegio().getId()));
    }

    /** Mismo mensaje para el flujo real y el reenvio de prueba. */
    private int avisarPedidoListo(PedidoListoEvent evento) {
        List<String> tokens = dispositivoRepository.tokensDeColegio(evento.colegioId(), Usuario.Rol.INSTITUCION);
        return envioPush.enviar(
                tokens,
                "¡Tu pedido está listo!",
                "El pedido " + evento.numeroPedido() + " ya está listo para entrega.",
                Map.of("tipo", "PEDIDO_LISTO", "pedidoId", String.valueOf(evento.pedidoId())));
    }
}
