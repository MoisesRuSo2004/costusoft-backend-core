package com.costusoft.inventory_system.module.usuario.mapper;

import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.module.usuario.dto.UsuarioDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UsuarioMapper {

    // ── CreateRequest → Entidad ──────────────────────────────────────────
    // password y colegio se resuelven en el service — el mapper solo copia campos planos

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "password",       ignore = true) // hash inutilizable en service
    @Mapping(target = "cuentaActivada", ignore = true) // siempre false al crear
    @Mapping(target = "colegio",        ignore = true) // resuelto en service por colegioId
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "updatedAt",      ignore = true)
    Usuario toEntity(UsuarioDTO.CreateRequest request);

    // ── Entidad → Response ────────────────────────────────────────────────
    // rol: enum → String
    // colegio: extrae id y nombre si existe

    @Mapping(target = "rol",
             expression = "java(usuario.getRol() != null ? usuario.getRol().name() : null)")
    @Mapping(target = "colegioId",
             expression = "java(usuario.getColegio() != null ? usuario.getColegio().getId() : null)")
    @Mapping(target = "nombreColegio",
             expression = "java(usuario.getColegio() != null ? usuario.getColegio().getNombre() : null)")
    @Mapping(target = "createdAt",
             expression = "java(formatDateTime(usuario.getCreatedAt()))")
    @Mapping(target = "updatedAt",
             expression = "java(formatDateTime(usuario.getUpdatedAt()))")
    UsuarioDTO.Response toResponse(Usuario usuario);

    // ── Helper ────────────────────────────────────────────────────────────

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
