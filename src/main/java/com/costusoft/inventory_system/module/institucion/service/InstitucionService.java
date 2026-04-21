package com.costusoft.inventory_system.module.institucion.service;

import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.module.ia.dto.IaDTO;
import com.costusoft.inventory_system.module.institucion.dto.InstitucionDTO;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contrato del portal institucional.
 *
 * Todos los metodos reciben el username del coordinador autenticado para
 * resolver su colegio internamente. Ninguna operacion permite operar
 * sobre datos de un colegio distinto al propio.
 */
public interface InstitucionService {

    // ── Perfil e informacion del colegio ─────────────────────────────────

    /**
     * Retorna el perfil del coordinador: datos del colegio, contadores
     * de pedidos activos, uniformes y solicitudes pendientes.
     */
    InstitucionDTO.PerfilResponse getPerfil(String username);

    // ── Pedidos ───────────────────────────────────────────────────────────

    /**
     * Lista los pedidos del colegio del coordinador, paginados.
     */
    PageDTO<PedidoDTO.Response> listarPedidos(String username, Pageable pageable);

    /**
     * Crea un nuevo pedido para el colegio del coordinador.
     * El colegioId se inyecta automaticamente — el coordinador
     * no puede crear pedidos para otros colegios.
     */
    PedidoDTO.Response crearPedido(InstitucionDTO.PedidoRequest request, String username);

    /**
     * Obtiene el detalle de un pedido. Verifica que pertenezca al colegio.
     */
    PedidoDTO.Response obtenerPedido(Long pedidoId, String username);

    /**
     * Historial de cambios de estado de un pedido.
     */
    List<PedidoDTO.HistorialResponse> obtenerHistorialPedido(Long pedidoId, String username);

    // ── Catalogo ─────────────────────────────────────────────────────────

    /**
     * Lista las prendas (uniformes) configuradas para el colegio,
     * con sus tallas e insumos requeridos.
     */
    List<InstitucionDTO.CatalogoItem> getCatalogo(String username);

    // ── Solicitudes especiales ────────────────────────────────────────────

    /**
     * Lista las solicitudes del colegio.
     * Si estado != null filtra por ese estado.
     */
    PageDTO<InstitucionDTO.SolicitudResponse> listarSolicitudes(
            String username, EstadoSolicitud estado, Pageable pageable);

    /**
     * Crea una nueva solicitud especial.
     */
    InstitucionDTO.SolicitudResponse crearSolicitud(
            InstitucionDTO.SolicitudRequest request, String username);

    // ── PEDIDO POR GRADO & SEGUIMIENTO ────────────────────────────────────

    /**
     * Genera una plantilla de pedido a partir de un grado + cantidad de estudiantes.
     * No crea el pedido definitivo; retorna los uniformes sugeridos y tallas disponibles
     * para que el frontend distribuya tallas y construya el PedidoRequest final.
     */
    InstitucionDTO.PedidoPorGradoResponse crearPedidoPorGrado(
            InstitucionDTO.PedidoPorGradoRequest request, String username);

    /**
     * Obtiene un resumen de seguimiento del pedido: estado, resumen de insumos faltantes
     * y referencia a la Salida (si existe). Verifica que el pedido pertenezca al colegio.
     */
    InstitucionDTO.SeguimientoResponse obtenerSeguimientoPedido(Long pedidoId, String username);

    // ── IA ────────────────────────────────────────────────────────────────

    /**
     * Chat libre con el asistente IA.
     * El contexto se enriquece con informacion del colegio del coordinador
     * (sus pedidos, uniformes, estado de solicitudes).
     */
    IaDTO.ChatResponse chatIa(IaDTO.ChatRequest request, String username);
}
