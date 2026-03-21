package com.costusoft.inventory_system.module.colegio.mapper;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.Uniforme;
import com.costusoft.inventory_system.module.colegio.dto.ColegioDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ColegioMapper {

    // ── Request → Entidad ────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uniformes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Colegio toEntity(ColegioDTO.Request request);

    // ── Entidad → Response (con conteo de uniformes) ─────────────────────

    @Mapping(target = "totalUniformes", expression = "java(colegio.getUniformes() != null ? colegio.getUniformes().size() : 0)")
    @Mapping(target = "createdAt", expression = "java(formatDateTime(colegio.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(formatDateTime(colegio.getUpdatedAt()))")
    ColegioDTO.Response toResponse(Colegio colegio);

    // ── Entidad → Response con uniformes ─────────────────────────────────

    @Mapping(target = "createdAt", expression = "java(formatDateTime(colegio.getCreatedAt()))")
    ColegioDTO.ResponseConUniformes toResponseConUniformes(Colegio colegio);

    // ── Uniforme → UniformeResumen ────────────────────────────────────────

    ColegioDTO.UniformeResumen uniformeToResumen(Uniforme uniforme);

    // ── Update parcial ────────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uniformes", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ColegioDTO.Request request, @MappingTarget Colegio colegio);

    // ── Helper ───────────────────────────────────────────────────────────

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}