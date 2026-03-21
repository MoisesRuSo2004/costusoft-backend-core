package com.costusoft.inventory_system.module.insumo.mapper;

import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.module.insumo.dto.InsumoDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper MapStruct para el modulo Insumo.
 *
 * MapStruct genera la implementacion en tiempo de compilacion,
 * eliminando el codigo boilerplate de conversion manual.
 *
 * componentModel = "spring" -> el mapper se registra como @Bean
 * y se puede inyectar con @Autowired / constructor injection.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InsumoMapper {

    // ── Request → Entidad ────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "riesgo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "stockMinimo", source = "stockMinimo", defaultExpression = "java(10)")
    Insumo toEntity(InsumoDTO.Request request);

    // ── Entidad → Response ───────────────────────────────────────────────

    @Mapping(target = "riesgo", expression = "java(insumo.getRiesgo() != null ? insumo.getRiesgo().name() : null)")
    @Mapping(target = "stockBajo", expression = "java(insumo.tieneStockBajo())")
    @Mapping(target = "createdAt", expression = "java(formatDateTime(insumo.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(formatDateTime(insumo.getUpdatedAt()))")
    InsumoDTO.Response toResponse(Insumo insumo);

    // ── Update parcial: Request → Entidad existente (ignora nulos) ───────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "riesgo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(InsumoDTO.Request request, @MappingTarget Insumo insumo);

    // ── Helper de formato ────────────────────────────────────────────────

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}