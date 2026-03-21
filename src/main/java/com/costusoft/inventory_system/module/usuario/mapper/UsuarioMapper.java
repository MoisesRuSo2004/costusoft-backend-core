package com.costusoft.inventory_system.module.usuario.mapper;

import com.costusoft.inventory_system.entity.Usuario;
import com.costusoft.inventory_system.module.usuario.dto.UsuarioDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UsuarioMapper {

    // ── CreateRequest → Entidad ──────────────────────────────────────────
    // password se hashea en el service — el mapper solo copia campos

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true) // se setea manualmente en service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Usuario toEntity(UsuarioDTO.CreateRequest request);

    // ── Entidad → Response ────────────────────────────────────────────────
    // rol se convierte de enum a String con .name()

    @Mapping(target = "rol", expression = "java(usuario.getRol() != null ? usuario.getRol().name() : null)")
    @Mapping(target = "createdAt", expression = "java(formatDateTime(usuario.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(formatDateTime(usuario.getUpdatedAt()))")
    UsuarioDTO.Response toResponse(Usuario usuario);

    // ── Helper ────────────────────────────────────────────────────────────

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
