package com.costusoft.inventory_system.module.uniforme.mapper;

import com.costusoft.inventory_system.entity.Uniforme;
import com.costusoft.inventory_system.entity.UniformeInsumo;
import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UniformeMapper {

    // ── Entidad → Response ───────────────────────────────────────────────

    @Mapping(target = "colegioId", source = "colegio.id")
    @Mapping(target = "colegioNombre", source = "colegio.nombre")
    @Mapping(target = "createdAt", expression = "java(formatDateTime(uniforme.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(formatDateTime(uniforme.getUpdatedAt()))")
    UniformeDTO.Response toResponse(Uniforme uniforme);

    // ── UniformeInsumo → InsumoRequeridoResponse ──────────────────────────

    @Mapping(target = "insumoId", source = "insumo.id")
    @Mapping(target = "nombreInsumo", source = "insumo.nombre")
    UniformeDTO.InsumoRequeridoResponse insumoRequeridoToResponse(UniformeInsumo uniformeInsumo);

    // ── Helper ────────────────────────────────────────────────────────────

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
