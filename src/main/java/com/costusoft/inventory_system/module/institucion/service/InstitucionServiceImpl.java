package com.costusoft.inventory_system.module.institucion.service;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.EstadoPedido;
import com.costusoft.inventory_system.entity.SolicitudEspecial;
import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.ia.dto.IaDTO;
import com.costusoft.inventory_system.module.ia.service.IaService;
import com.costusoft.inventory_system.module.institucion.dto.InstitucionDTO;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.costusoft.inventory_system.module.pedido.service.PedidoService;
import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;
import com.costusoft.inventory_system.module.uniforme.service.UniformeService;
import com.costusoft.inventory_system.repo.PedidoRepository;
import com.costusoft.inventory_system.repo.SolicitudEspecialRepository;
import com.costusoft.inventory_system.repo.UsuarioRepository;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementacion del portal institucional.
 *
 * Principios de seguridad:
 * - Todos los metodos resuelven el colegio desde el username autenticado
 * - Ninguna operacion permite acceder a datos de otro colegio
 * - Los pedidos creados aqui siempre usan el colegioId del coordinador
 * - Las solicitudes siempre se asocian al colegio del coordinador
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InstitucionServiceImpl implements InstitucionService {

        private final UsuarioRepository usuarioRepository;
        private final SolicitudEspecialRepository solicitudRepository;
        private final PedidoRepository pedidoRepository;
        private final PedidoService pedidoService;
        private final UniformeService uniformeService;
        private final IaService iaService;

        /** Estados finales: no cuentan como pedidos "activos" */
        private static final java.util.List<EstadoPedido> ESTADOS_FINALES = java.util.List.of(EstadoPedido.ENTREGADO,
                        EstadoPedido.CANCELADO);

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // ── Perfil ─────────────────────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public InstitucionDTO.PerfilResponse getPerfil(String username) {
                Usuario usuario = cargarUsuarioConColegio(username);
                Colegio colegio = usuario.getColegio();

                // Contadores para el dashboard — queries directas, sin cargar entidades
                // completas
                long totalPedidos = pedidoRepository.countByColegioId(colegio.getId());

                long pedidosActivos = pedidoRepository
                                .countByColegioIdAndEstadoNotIn(colegio.getId(), ESTADOS_FINALES);

                int totalUniformes = uniformeService.listarPorColegio(colegio.getId()).size();

                long solicitudesPendientes = solicitudRepository
                                .countByColegioAndEstado(colegio, EstadoSolicitud.PENDIENTE);

                log.debug("Perfil institucional cargado para '{}' — colegio id: {}",
                                username, colegio.getId());

                return InstitucionDTO.PerfilResponse.builder()
                                .colegioId(colegio.getId())
                                .nombreColegio(colegio.getNombre())
                                .direccionColegio(colegio.getDireccion())
                                .username(usuario.getUsername())
                                .correo(usuario.getCorreo())
                                .totalPedidos((int) totalPedidos)
                                .pedidosActivos((int) pedidosActivos)
                                .totalUniformes(totalUniformes)
                                .solicitudesPendientes((int) solicitudesPendientes)
                                .build();
        }

        // ── Pedidos ────────────────────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public PageDTO<PedidoDTO.Response> listarPedidos(String username, Pageable pageable) {
                Colegio colegio = cargarColegio(username);
                return pedidoService.listarPorColegio(colegio.getId(), pageable);
        }

        @Override
        public PedidoDTO.Response crearPedido(InstitucionDTO.PedidoRequest request,
                        String username) {
                Colegio colegio = cargarColegio(username);

                // Construir el PedidoDTO.Request inyectando el colegioId del coordinador
                PedidoDTO.Request pedidoRequest = new PedidoDTO.Request();
                pedidoRequest.setColegioId(colegio.getId());
                pedidoRequest.setFechaEstimadaEntrega(request.getFechaEstimadaEntrega());
                pedidoRequest.setObservaciones(request.getObservaciones());
                pedidoRequest.setDetalles(request.getDetalles());

                // Validar que los uniformes pertenezcan al colegio del coordinador
                validarUniformesDelColegio(request.getDetalles(), colegio.getId());

                log.info("Pedido creado por coordinador '{}' para colegio '{}'",
                                username, colegio.getNombre());

                return pedidoService.crear(pedidoRequest, username);
        }

        @Override
        @Transactional(readOnly = true)
        public PedidoDTO.Response obtenerPedido(Long pedidoId, String username) {
                Colegio colegio = cargarColegio(username);
                PedidoDTO.Response pedido = pedidoService.obtenerPorId(pedidoId);

                // Seguridad: verificar que el pedido pertenece al colegio del coordinador
                if (!pedido.getColegio().getId().equals(colegio.getId())) {
                        throw new BusinessException("No tienes permiso para acceder a este pedido.");
                }

                return pedido;
        }

        @Override
        @Transactional(readOnly = true)
        public List<PedidoDTO.HistorialResponse> obtenerHistorialPedido(Long pedidoId,
                        String username) {
                // Validar acceso antes de retornar el historial
                obtenerPedido(pedidoId, username);
                return pedidoService.obtenerHistorial(pedidoId);
        }

        // ── PEDIDO POR GRADO & SEGUIMIENTO ─────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public InstitucionDTO.PedidoPorGradoResponse crearPedidoPorGrado(InstitucionDTO.PedidoPorGradoRequest request,
                        String username) {
                Colegio colegio = cargarColegio(username);

                // Obtener uniformes del colegio y filtrar por tipo si se especifica
                List<UniformeDTO.Response> uniformes = uniformeService.listarPorColegio(colegio.getId());
                if (request.getTipoUniforme() != null && !request.getTipoUniforme().isBlank()) {
                        String tipoNorm = request.getTipoUniforme().trim();
                        uniformes = uniformes.stream()
                                        .filter(u -> tipoNorm.equalsIgnoreCase(u.getTipo() == null ? "" : u.getTipo()))
                                        .toList();
                }

                List<InstitucionDTO.PedidoPorGradoItem> items = uniformes.stream()
                                .map(u -> InstitucionDTO.PedidoPorGradoItem.builder()
                                                .uniformeId(u.getId())
                                                .nombre(u.getPrenda())
                                                .tipo(u.getTipo())
                                                .genero(u.getGenero())
                                                .cantidadSugerida(request.getCantidadEstudiantes())
                                                .tallasDisponibles(u.getTallas())
                                                .build())
                                .toList();

                return InstitucionDTO.PedidoPorGradoResponse.builder()
                                .colegioId(colegio.getId())
                                .colegioNombre(colegio.getNombre())
                                .grado(request.getGrado())
                                .cantidadEstudiantes(request.getCantidadEstudiantes())
                                .items(items)
                                .fechaEstimadaEntrega(request.getFechaEstimadaEntrega() != null
                                                ? request.getFechaEstimadaEntrega().toString()
                                                : null)
                                .observaciones(request.getObservaciones())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public InstitucionDTO.SeguimientoResponse obtenerSeguimientoPedido(Long pedidoId, String username) {
                // Reutiliza obtenerPedido() para validar acceso
                PedidoDTO.Response pedido = obtenerPedido(pedidoId, username);

                return InstitucionDTO.SeguimientoResponse.builder()
                                .pedidoId(pedido.getId())
                                .numeroPedido(pedido.getNumeroPedido())
                                .estado(pedido.getEstado())
                                .estadoDescripcion(pedido.getEstadoDescripcion())
                                .fechaEstimadaEntrega(pedido.getFechaEstimadaEntrega())
                                .salidaId(pedido.getSalidaId())
                                .resumenInsumos(pedido.getResumenInsumos())
                                .build();
        }

        // ── Catalogo ───────────────────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public List<InstitucionDTO.CatalogoItem> getCatalogo(String username) {
                Colegio colegio = cargarColegio(username);

                return uniformeService.listarPorColegio(colegio.getId()).stream()
                                .map(this::toCatalogoItem)
                                .toList();
        }

        // ── Solicitudes especiales ─────────────────────────────────────────────

        @Override
        @Transactional(readOnly = true)
        public PageDTO<InstitucionDTO.SolicitudResponse> listarSolicitudes(
                        String username, EstadoSolicitud estado, Pageable pageable) {

                Colegio colegio = cargarColegio(username);

                Page<SolicitudEspecial> page = (estado != null)
                                ? solicitudRepository.findByColegioAndEstadoOrderByCreatedAtDesc(
                                                colegio, estado, pageable)
                                : solicitudRepository.findByColegioOrderByCreatedAtDesc(
                                                colegio, pageable);

                return PageDTO.from(page, this::toSolicitudResponse);
        }

        @Override
        public InstitucionDTO.SolicitudResponse crearSolicitud(
                        InstitucionDTO.SolicitudRequest request, String username) {

                Usuario usuario = cargarUsuarioConColegio(username);
                Colegio colegio = usuario.getColegio();

                SolicitudEspecial solicitud = SolicitudEspecial.builder()
                                .usuario(usuario)
                                .colegio(colegio)
                                .tipo(request.getTipo())
                                .estado(EstadoSolicitud.PENDIENTE)
                                .asunto(request.getAsunto())
                                .descripcion(request.getDescripcion())
                                .build();

                SolicitudEspecial guardada = solicitudRepository.save(solicitud);

                log.info("Solicitud especial creada — id: {} | tipo: {} | colegio: '{}'",
                                guardada.getId(), guardada.getTipo(), colegio.getNombre());

                return toSolicitudResponse(guardada);
        }

        // ── IA ─────────────────────────────────────────────────────────────────

        @Override
        public IaDTO.ChatResponse chatIa(IaDTO.ChatRequest request, String username) {
                Colegio colegio = cargarColegio(username);

                log.debug("Chat IA institucional para '{}' — colegio '{}'",
                                username, colegio.getNombre());

                // Construir contexto reducido y focalizado para este colegio: últimos 5 pedidos
                // + conteo de prendas
                var ultimosPedidos = pedidoService.listarPorColegio(colegio.getId(),
                                org.springframework.data.domain.PageRequest.of(0, 5));

                StringBuilder contexto = new StringBuilder();
                contexto.append("{\n");
                contexto.append(String.format("  \"colegio\": { \"id\": %d, \"nombre\": \"%s\" },\n", colegio.getId(),
                                colegio.getNombre()));
                contexto.append(String.format("  \"totalUniformes\": %d,\n",
                                uniformeService.listarPorColegio(colegio.getId()).size()));

                contexto.append("  \"ultimosPedidos\": [\n");
                for (var p : ultimosPedidos.getContent()) {
                        contexto.append(String.format(
                                        "    { \"id\": %d, \"numeroPedido\": \"%s\", \"estado\": \"%s\", \"fechaEstimadaEntrega\": \"%s\", \"porcentajeCumplimiento\": %s },\n",
                                        p.getId(), p.getNumeroPedido(), p.getEstado(), p.getFechaEstimadaEntrega(),
                                        p.getPorcentajeCumplimiento()));
                }
                contexto.append("  ]\n");
                contexto.append("}\n");

                // Llamar a la IA pasando el contexto estructurado
                return iaService.chatWithContext(contexto.toString(), request);
        }

        // ── Helpers privados ───────────────────────────────────────────────────

        /**
         * Carga el usuario con su colegio. Valida que sea INSTITUCION y tenga colegio.
         */
        private Usuario cargarUsuarioConColegio(String username) {
                Usuario usuario = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario", "username", username));

                if (usuario.getRol() != Usuario.Rol.INSTITUCION) {
                        throw new BusinessException("Acceso restringido al portal institucional.");
                }

                if (usuario.getColegio() == null) {
                        throw new BusinessException(
                                        "Tu cuenta no tiene un colegio asignado. Contacta al administrador.");
                }

                return usuario;
        }

        /** Shortcut — solo necesita el colegio */
        private Colegio cargarColegio(String username) {
                return cargarUsuarioConColegio(username).getColegio();
        }

        /**
         * Valida que todos los uniformes de los detalles pertenezcan al colegio.
         * Previene que un coordinador solicite prendas de otro colegio.
         */
        private void validarUniformesDelColegio(
                        List<com.costusoft.inventory_system.module.pedido.dto.PedidoDTO.DetalleRequest> detalles,
                        Long colegioId) {

                List<UniformeDTO.Response> uniformesColegio = uniformeService.listarPorColegio(colegioId);

                java.util.Set<Long> idsValidos = uniformesColegio.stream()
                                .map(UniformeDTO.Response::getId)
                                .collect(java.util.stream.Collectors.toSet());

                detalles.forEach(detalle -> {
                        if (!idsValidos.contains(detalle.getUniformeId())) {
                                throw new BusinessException(
                                                "El uniforme con id " + detalle.getUniformeId()
                                                                + " no pertenece al catalogo de tu colegio.");
                        }
                });
        }

        /** Transforma UniformeDTO.Response → InstitucionDTO.CatalogoItem */
        private InstitucionDTO.CatalogoItem toCatalogoItem(UniformeDTO.Response uniforme) {
                List<InstitucionDTO.InsumoInfo> insumos = uniforme.getInsumosRequeridos()
                                .stream()
                                .map(ir -> InstitucionDTO.InsumoInfo.builder()
                                                .insumoId(ir.getInsumoId())
                                                .nombre(ir.getNombreInsumo())
                                                .unidadMedida(ir.getUnidadMedida())
                                                .build())
                                .distinct()
                                .toList();

                return InstitucionDTO.CatalogoItem.builder()
                                .uniformeId(uniforme.getId())
                                .nombre(uniforme.getPrenda())
                                .tipo(uniforme.getTipo())
                                .genero(uniforme.getGenero())
                                .tallas(uniforme.getTallas())
                                .insumos(insumos)
                                .build();
        }

        /** Transforma SolicitudEspecial → InstitucionDTO.SolicitudResponse */
        private InstitucionDTO.SolicitudResponse toSolicitudResponse(SolicitudEspecial s) {
                return InstitucionDTO.SolicitudResponse.builder()
                                .id(s.getId())
                                .tipo(s.getTipo().name())
                                .estado(s.getEstado().name())
                                .asunto(s.getAsunto())
                                .descripcion(s.getDescripcion())
                                .respuesta(s.getRespuesta())
                                .fechaRespuesta(s.getFechaRespuesta() != null
                                                ? s.getFechaRespuesta().format(FMT)
                                                : null)
                                .createdAt(s.getCreatedAt() != null
                                                ? s.getCreatedAt().format(FMT)
                                                : null)
                                .build();
        }
}
