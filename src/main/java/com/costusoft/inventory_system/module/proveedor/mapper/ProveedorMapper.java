package com.costusoft.inventory_system.module.proveedor.mapper;

import com.costusoft.inventory_system.entity.Proveedor;
import com.costusoft.inventory_system.module.proveedor.dto.ProveedorDTO;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProveedorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Proveedor toEntity(ProveedorDTO.Request request);

    @Mapping(target = "createdAt", expression = "java(formatDateTime(proveedor.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(formatDateTime(proveedor.getUpdatedAt()))")
    ProveedorDTO.Response toResponse(Proveedor proveedor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ProveedorDTO.Request request, @MappingTarget Proveedor proveedor);

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}