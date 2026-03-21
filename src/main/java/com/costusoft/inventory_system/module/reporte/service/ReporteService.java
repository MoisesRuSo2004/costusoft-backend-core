package com.costusoft.inventory_system.module.reporte.service;

import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;

import java.io.ByteArrayInputStream;

public interface ReporteService {

    /** Genera el reporte en memoria como lista de items + resumen */
    ReporteDTO.Response generarReporte(ReporteDTO.FiltroRequest filtro);

    /** Exporta el reporte como archivo PDF */
    ByteArrayInputStream exportarPdf(ReporteDTO.FiltroRequest filtro);

    /** Exporta el reporte como archivo Excel (.xlsx) */
    ByteArrayInputStream exportarExcel(ReporteDTO.FiltroRequest filtro);
}
