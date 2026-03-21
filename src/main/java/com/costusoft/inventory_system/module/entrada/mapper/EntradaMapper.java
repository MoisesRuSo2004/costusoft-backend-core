package com.costusoft.inventory_system.module.entrada.mapper;

import com.costusoft.inventory_system.entity.DetalleEntrada;
import com.costusoft.inventory_system.entity.Entrada;
import com.costusoft.inventory_system.module.entrada.dto.EntradaDTO;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper de Entrada.
 *
 * La construccion de la entidad completa (con detalles e insumos resueltos)
 * se hace en el Service — el mapper solo convierte campos simples
 * para mantener la responsabilidad separada.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EntradaMapper {

    // ── Entidad → Response ───────────────────────────────────────────────

    @Mapping(target = "fecha", expression = "java(formatDate(entrada.getFecha()))")
    @Mapping(target = "proveedorNombre", expression = "java(entrada.getProveedor() != null ? entrada.getProveedor().getNombre() : null)")
    @Mapping(target = "createdAt", expression = "java(formatDateTime(entrada.getCreatedAt()))")
    EntradaDTO.Response toResponse(Entrada entrada);

    // ── DetalleEntrada → DetalleResponse ─────────────────────────────────

    @Mapping(target = "insumoId", source = "insumo.id")
    @Mapping(target = "nombreInsumo", expression = "java(detalle.getNombreInsumoSnapshot() != null ? detalle.getNombreInsumoSnapshot() : detalle.getInsumo().getNombre())")
    @Mapping(target = "unidadMedida", source = "insumo.unidadMedida")
    EntradaDTO.DetalleResponse detalleToResponse(DetalleEntrada detalle);

    // ── Helpers ──────────────────────────────────────────────────────────

    default String formatDate(LocalDate date) {
        if (date == null)
            return null;
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}