package com.costusoft.inventory_system.module.salida.mapper;

import com.costusoft.inventory_system.entity.DetalleSalida;
import com.costusoft.inventory_system.module.salida.dto.SalidaDTO;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SalidaMapper {

    @Mapping(target = "insumoId", source = "insumo.id")
    @Mapping(target = "nombreInsumo", expression = "java(detalle.getNombreInsumoSnapshot() != null ? detalle.getNombreInsumoSnapshot() : detalle.getInsumo().getNombre())")
    @Mapping(target = "unidadMedida", source = "insumo.unidadMedida")
    SalidaDTO.DetalleResponse detalleToResponse(DetalleSalida detalle);

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