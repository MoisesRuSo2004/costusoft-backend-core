package com.costusoft.inventory_system.module.uniforme.mapper;

import com.costusoft.inventory_system.entity.UniformeInsumo;
import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UniformeMapper {

    // ── UniformeInsumo → InsumoRequeridoResponse ──────────────────────────
    // MapStruct auto-mapea: id, cantidadBase, unidadMedida, talla
    // Solo se necesita expresar las que tienen fuente distinta

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
