package com.costusoft.inventory_system.module.institucion.controllers;

import com.costusoft.inventory_system.entity.SolicitudEspecial.EstadoSolicitud;
import com.costusoft.inventory_system.module.ia.dto.IaDTO;
import com.costusoft.inventory_system.module.institucion.dto.InstitucionDTO;
import com.costusoft.inventory_system.module.institucion.service.InstitucionService;
import com.costusoft.inventory_system.module.pedido.dto.PedidoDTO;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import com.costusoft.inventory_system.shared.dto.PageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Portal institucional — endpoints exclusivos para coordinadores de colegio.
 *
 * Todos los endpoints requieren rol INSTITUCION.
 * El colegio del coordinador se resuelve automaticamente desde su JWT,
 * garantizando aislamiento total entre instituciones.
 *
 * Base URL: /api/institucion
 *
 * ┌────────────────────────────────────────────────────────────────────┐
 * │ Seccion │ Endpoints │
 * ├────────────────────────────────────────────────────────────────────┤
 * │ Perfil │ GET /perfil │
 * │ Pedidos │ GET /pedidos │
 * │ │ POST /pedidos │
 * │ │ GET /pedidos/{id} │
 * │ │ GET /pedidos/{id}/historial │
 * │ Catalogo │ GET /catalogo │
 * │ Solicitudes │ GET /solicitudes │
 * │ │ POST /solicitudes │
 * │ IA │ POST /ia/chat │
 * └────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/institucion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INSTITUCION')")
@Tag(name = "Portal Institucional", description = "Endpoints del portal para coordinadores de colegio. "
                + "Acceso restringido a rol INSTITUCION.")
public class InstitucionController {

        private final InstitucionService institucionService;

        // ══════════════════════════════════════════════════════════════════════
        // PERFIL
        // ══════════════════════════════════════════════════════════════════════

        @Operation(summary = "Perfil del coordinador", description = "Retorna los datos del colegio del coordinador autenticado, "
                        + "junto con contadores de pedidos activos, uniformes y solicitudes pendientes.")
        @GetMapping("/perfil")
        public ResponseEntity<ApiResponse<InstitucionDTO.PerfilResponse>> getPerfil(
                        Authentication auth) {

                InstitucionDTO.PerfilResponse perfil = institucionService.getPerfil(auth.getName());

                return ResponseEntity.ok(
                                ApiResponse.ok("Perfil institucional", perfil));
        }

        // ══════════════════════════════════════════════════════════════════════
        // PEDIDOS
        // ══════════════════════════════════════════════════════════════════════

        @Operation(summary = "Listar pedidos del colegio", description = "Retorna todos los pedidos del colegio del coordinador, paginados. "
                        + "Ordenados por fecha de creacion descendente.")
        @GetMapping("/pedidos")
        public ResponseEntity<ApiResponse<PageDTO<PedidoDTO.Response>>> listarPedidos(
                        @Parameter(description = "Pagina (0-indexado)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Elementos por pagina") @RequestParam(defaultValue = "10") int size,
                        Authentication auth) {

                Pageable pageable = PageRequest.of(page, size,
                                Sort.by("createdAt").descending());

                PageDTO<PedidoDTO.Response> resultado = institucionService.listarPedidos(auth.getName(), pageable);

                return ResponseEntity.ok(
                                ApiResponse.ok("Pedidos del colegio", resultado));
        }

        @Operation(summary = "Crear pedido", description = "Crea un nuevo pedido en estado BORRADOR para el colegio del coordinador. "
                        + "El colegioId se asigna automaticamente — no se puede crear pedidos para otros colegios. "
                        + "Los uniformes deben pertenecer al catalogo del propio colegio.")
        @PostMapping("/pedidos")
        public ResponseEntity<ApiResponse<PedidoDTO.Response>> crearPedido(
                        @Valid @RequestBody InstitucionDTO.PedidoRequest request,
                        Authentication auth) {

                PedidoDTO.Response pedido = institucionService.crearPedido(request, auth.getName());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("Pedido creado exitosamente", pedido));
        }

        @Operation(summary = "Detalle de un pedido", description = "Obtiene el detalle completo de un pedido. "
                        + "Solo es accesible si el pedido pertenece al colegio del coordinador.")
        @GetMapping("/pedidos/{id}")
        public ResponseEntity<ApiResponse<PedidoDTO.Response>> obtenerPedido(
                        @Parameter(description = "ID del pedido") @PathVariable Long id,
                        Authentication auth) {

                PedidoDTO.Response pedido = institucionService.obtenerPedido(id, auth.getName());

                return ResponseEntity.ok(ApiResponse.ok("Pedido", pedido));
        }

        @Operation(summary = "Historial de cambios de un pedido", description = "Retorna el historial de transiciones de estado del pedido, "
                        + "incluyendo quien realizo cada cambio y la fecha.")
        @GetMapping("/pedidos/{id}/historial")
        public ResponseEntity<ApiResponse<List<PedidoDTO.HistorialResponse>>> obtenerHistorial(
                        @Parameter(description = "ID del pedido") @PathVariable Long id,
                        Authentication auth) {

                List<PedidoDTO.HistorialResponse> historial = institucionService.obtenerHistorialPedido(id,
                                auth.getName());

                return ResponseEntity.ok(
                                ApiResponse.ok("Historial del pedido", historial));
        }

        // ══════════════════════════════════════════════════════════════════════
        // CATALOGO
        // ══════════════════════════════════════════════════════════════════════

        @Operation(summary = "Catalogo de prendas del colegio", description = "Lista todas las prendas (uniformes) configuradas para el colegio del coordinador, "
                        + "con las tallas disponibles e insumos requeridos para cada una. "
                        + "Util para poblar el formulario de creacion de pedido.")
        @GetMapping("/catalogo")
        public ResponseEntity<ApiResponse<List<InstitucionDTO.CatalogoItem>>> getCatalogo(
                        Authentication auth) {

                List<InstitucionDTO.CatalogoItem> catalogo = institucionService.getCatalogo(auth.getName());

                return ResponseEntity.ok(
                                ApiResponse.ok("Catalogo de prendas (" + catalogo.size() + ")", catalogo));
        }

        // ══════════════════════════════════════════════════════════════════════
        // SOLICITUDES ESPECIALES
        // ══════════════════════════════════════════════════════════════════════

        @Operation(summary = "Listar solicitudes especiales", description = "Retorna las solicitudes especiales del colegio, paginadas. "
                        + "Se puede filtrar por estado (PENDIENTE, EN_REVISION, RESUELTA, RECHAZADA). "
                        + "Ordenadas por fecha de creacion descendente.")
        @GetMapping("/solicitudes")
        public ResponseEntity<ApiResponse<PageDTO<InstitucionDTO.SolicitudResponse>>> listarSolicitudes(
                        @Parameter(description = "Filtrar por estado. Si no se indica, retorna todos.") @RequestParam(required = false) EstadoSolicitud estado,
                        @Parameter(description = "Pagina (0-indexado)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Elementos por pagina") @RequestParam(defaultValue = "10") int size,
                        Authentication auth) {

                Pageable pageable = PageRequest.of(page, size);
                PageDTO<InstitucionDTO.SolicitudResponse> resultado = institucionService
                                .listarSolicitudes(auth.getName(), estado, pageable);

                return ResponseEntity.ok(
                                ApiResponse.ok("Solicitudes del colegio", resultado));
        }

        @Operation(summary = "Crear solicitud especial", description = "Envia una nueva solicitud especial al equipo Costusoft. "
                        + "Tipos disponibles: AJUSTE_TALLA, PEDIDO_URGENTE, CAMBIO_FECHA_ENTREGA, "
                        + "CONSULTA_GENERAL, DEVOLUCION. "
                        + "La solicitud queda en estado PENDIENTE hasta que sea gestionada.")
        @PostMapping("/solicitudes")
        public ResponseEntity<ApiResponse<InstitucionDTO.SolicitudResponse>> crearSolicitud(
                        @Valid @RequestBody InstitucionDTO.SolicitudRequest request,
                        Authentication auth) {

                InstitucionDTO.SolicitudResponse solicitud = institucionService.crearSolicitud(request, auth.getName());

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok("Solicitud enviada exitosamente", solicitud));
        }

        // ══════════════════════════════════════════════════════════════════════
        // INTELIGENCIA ARTIFICIAL
        // ══════════════════════════════════════════════════════════════════════

        @Operation(summary = "Chat con asistente IA", description = "Permite al coordinador hacer preguntas en lenguaje natural sobre sus pedidos, "
                        + "estado de produccion y uniformes de su colegio. "
                        + "La IA recibe el nombre del colegio como contexto para respuestas mas precisas. "
                        + "Ejemplos de preguntas: "
                        + "'¿Cuándo estara listo mi ultimo pedido?' "
                        + "'¿Cuantos uniformes tengo configurados?' "
                        + "'¿En que estado esta el pedido de diciembre?'")
        @PostMapping("/ia/chat")
        public ResponseEntity<ApiResponse<IaDTO.ChatResponse>> chatIa(
                        @Valid @RequestBody IaDTO.ChatRequest request,
                        Authentication auth) {

                IaDTO.ChatResponse respuesta = institucionService.chatIa(request, auth.getName());

                return ResponseEntity.ok(
                                ApiResponse.ok("Respuesta del asistente", respuesta));
        }
}
