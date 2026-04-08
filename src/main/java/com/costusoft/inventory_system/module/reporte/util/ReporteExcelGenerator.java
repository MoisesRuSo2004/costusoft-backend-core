package com.costusoft.inventory_system.module.reporte.util;

import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * Generador de reportes en formato Excel (.xlsx) usando Apache POI.
 *
 * Tipos soportados:
 *   GENERAL / ENTRADAS / SALIDAS / STOCK_BAJO  → hoja "Inventario"
 *   ROTACION                                   → hoja "Rotación"
 *   CONSUMO_PROMEDIO                           → hoja "Consumo Promedio"
 *   PEDIDOS                                    → hoja "Pedidos"
 *
 * Estructura de cada hoja:
 *   1. Fila de título (merge)
 *   2. Info del filtro (periodo, fecha generación)
 *   3. Encabezados de tabla (fondo gris oscuro, texto blanco, negrita)
 *   4. Filas de datos (alternadas, alertas coloreadas)
 *   5. Sección de resumen
 *   6. Autosize de columnas
 */
@Slf4j
public class ReporteExcelGenerator {

    // ── Paleta de colores (RGB) ──────────────────────────────────────────
    private static final byte[] COL_VERDE    = { (byte)39,  (byte)174, (byte)96  };
    private static final byte[] COL_AMARILLO = { (byte)241, (byte)196, (byte)15  };
    private static final byte[] COL_ROJO     = { (byte)231, (byte)76,  (byte)60  };
    private static final byte[] COL_NARANJA  = { (byte)230, (byte)126, (byte)34  };
    private static final byte[] COL_AZUL     = { (byte)52,  (byte)152, (byte)219 };
    private static final byte[] COL_GRIS_ENC = { (byte)89,  (byte)89,  (byte)89  };
    private static final byte[] COL_TITULO   = { (byte)21,  (byte)67,  (byte)96  };
    private static final byte[] COL_GRIS_ALT = { (byte)242, (byte)242, (byte)242 };

    private ReporteExcelGenerator() {}

    // ════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════════════════════════════════

    public static ByteArrayInputStream generar(ReporteDTO.Response reporte) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            String tipo = reporte.getResumen().getTipoInforme();

            if (reporte.getRotacion() != null) {
                escribirHojaRotacion(wb, reporte);
            } else if (reporte.getConsumo() != null) {
                escribirHojaConsumo(wb, reporte);
            } else if (reporte.getPedidos() != null) {
                escribirHojaPedidos(wb, reporte);
            } else {
                escribirHojaInventario(wb, reporte);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            log.error("Error generando Excel para reporte {}: {}",
                    reporte.getResumen().getTipoInforme(), e.getMessage(), e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HOJAS POR TIPO
    // ════════════════════════════════════════════════════════════════════════

    // ── INVENTARIO (GENERAL / ENTRADAS / SALIDAS / STOCK_BAJO) ──────────

    private static void escribirHojaInventario(XSSFWorkbook wb, ReporteDTO.Response reporte) {
        Sheet sheet = wb.createSheet("Inventario");
        Estilos e = new Estilos(wb);

        int fila = 0;
        String[] cols = { "Insumo", "Tipo", "Unidad", "Entradas", "Salidas", "Stock actual", "Stock mínimo", "Estado" };

        fila = escribirCabecera(sheet, e, fila,
                "REPORTE DE INVENTARIO — " + reporte.getResumen().getTipoInforme(),
                cols.length, reporte.getResumen());

        // Encabezados
        fila = escribirEncabezados(sheet, e, fila, cols);

        // Datos
        boolean alt = false;
        for (ReporteDTO.ItemResponse item : reporte.getItems()) {
            Row row = sheet.createRow(fila++);
            String estado = item.isStockCero() ? "CRITICO" : item.isStockBajo() ? "BAJO" : "OK";
            byte[] colorEstado = item.isStockCero() ? COL_ROJO : item.isStockBajo() ? COL_NARANJA : COL_VERDE;

            CellStyle base = alt ? e.datoGris : e.datoNormal;
            celda(row, 0, item.getNombreInsumo(),     base);
            celda(row, 1, nvl(item.getTipo()),        base);
            celda(row, 2, item.getUnidadMedida(),     base);
            celdaNum(row, 3, item.getEntradas(),      base);
            celdaNum(row, 4, item.getSalidas(),       base);
            celdaNum(row, 5, item.getStockActual(),   base);
            celdaNum(row, 6, item.getStockMinimo(),   base);
            celdaColor(wb, row, 7, estado, colorEstado);
            alt = !alt;
        }

        // Resumen
        fila++;
        ReporteDTO.ResumenResponse res = reporte.getResumen();
        fila = filaResumen(sheet, e, fila, "Total insumos:",           res.getTotalInsumos());
        fila = filaResumen(sheet, e, fila, "Total entradas:",          res.getTotalEntradas());
        fila = filaResumen(sheet, e, fila, "Total salidas:",           res.getTotalSalidas());
        fila = filaResumen(sheet, e, fila, "Insumos con stock bajo:",  res.getInsumosConStockBajo());
             filaResumen(sheet, e, fila, "Insumos sin stock (cero):", res.getInsumosConStockCero());

        autosizeColumnas(sheet, cols.length);
    }

    // ── ROTACIÓN ──────────────────────────────────────────────────────────

    private static void escribirHojaRotacion(XSSFWorkbook wb, ReporteDTO.Response reporte) {
        Sheet sheet = wb.createSheet("Rotación");
        Estilos e = new Estilos(wb);

        String[] cols = { "Insumo", "Tipo", "Unidad", "Stock actual",
                          "Total salidas", "Índice rotación (u/mes)", "Días cobertura", "Categoría" };

        int fila = 0;
        fila = escribirCabecera(sheet, e, fila,
                "REPORTE DE ROTACIÓN DE INVENTARIO", cols.length, reporte.getResumen());
        fila = escribirEncabezados(sheet, e, fila, cols);

        boolean alt = false;
        for (ReporteDTO.RotacionItem item : reporte.getRotacion()) {
            Row row = sheet.createRow(fila++);
            CellStyle base = alt ? e.datoGris : e.datoNormal;

            byte[] colorCat;
            switch (nvl(item.getCategoriaRotacion())) {
                case "Alta rotación"  -> colorCat = COL_VERDE;
                case "Media rotación" -> colorCat = COL_AZUL;
                case "Baja rotación"  -> colorCat = COL_NARANJA;
                default               -> colorCat = COL_ROJO;   // Sin movimiento / stock muerto
            }

            celda(row, 0, item.getNombreInsumo(),                               base);
            celda(row, 1, nvl(item.getTipo()),                                  base);
            celda(row, 2, item.getUnidadMedida(),                               base);
            celdaNum(row, 3, item.getStockActual(),                             base);
            celdaNum(row, 4, item.getTotalSalidas(),                            base);
            celdaDec(row, 5, item.getIndiceRotacion(),                          base);
            celdaNumNullable(row, 6, item.getDiasCobertura(),                   base);
            celdaColor(wb, row, 7, item.getCategoriaRotacion(),                 colorCat);
            alt = !alt;
        }

        fila++;
        ReporteDTO.ResumenResponse res = reporte.getResumen();
        fila = filaResumen(sheet, e, fila, "Total insumos analizados:", res.getTotalInsumos());
        fila = filaResumen(sheet, e, fila, "Insumos alta rotación:",    res.getInsumosAltaRotacion());
             filaResumen(sheet, e, fila, "Insumos stock muerto:",      res.getInsumosStockMuerto());

        autosizeColumnas(sheet, cols.length);
    }

    // ── CONSUMO PROMEDIO ──────────────────────────────────────────────────

    private static void escribirHojaConsumo(XSSFWorkbook wb, ReporteDTO.Response reporte) {
        Sheet sheet = wb.createSheet("Consumo Promedio");
        Estilos e = new Estilos(wb);

        String[] cols = { "Insumo", "Tipo", "Unidad", "Stock actual",
                          "Total consumo", "Consumo/día", "Consumo/mes", "Días cobertura est.", "Tendencia" };

        int fila = 0;
        fila = escribirCabecera(sheet, e, fila,
                "REPORTE DE CONSUMO PROMEDIO", cols.length, reporte.getResumen());
        fila = escribirEncabezados(sheet, e, fila, cols);

        boolean alt = false;
        for (ReporteDTO.ConsumoItem item : reporte.getConsumo()) {
            Row row = sheet.createRow(fila++);
            CellStyle base = alt ? e.datoGris : e.datoNormal;

            byte[] colorTend;
            switch (nvl(item.getTendencia())) {
                case "Creciente"   -> colorTend = COL_ROJO;      // alerta: consumo sube
                case "Decreciente" -> colorTend = COL_VERDE;
                case "Estable"     -> colorTend = COL_AZUL;
                default            -> colorTend = COL_GRIS_ENC;  // Sin datos
            }

            celda(row, 0, item.getNombreInsumo(),                             base);
            celda(row, 1, nvl(item.getTipo()),                                base);
            celda(row, 2, item.getUnidadMedida(),                             base);
            celdaNum(row, 3, item.getStockActual(),                           base);
            celdaNum(row, 4, item.getTotalConsumo(),                          base);
            celdaDec(row, 5, item.getConsumoDiario(),                         base);
            celdaDec(row, 6, item.getConsumoMensual(),                        base);
            celdaNumNullable(row, 7, item.getDiasCoberturaEstimados(),        base);
            celdaColor(wb, row, 8, nvl(item.getTendencia()),                  colorTend);
            alt = !alt;
        }

        fila++;
        ReporteDTO.ResumenResponse res = reporte.getResumen();
        fila = filaResumen(sheet, e, fila, "Total insumos analizados:",      res.getTotalInsumos());
        fila = filaResumen(sheet, e, fila, "Tendencia creciente:",           res.getInsumosTendenciaCreciente());
             filaResumen(sheet, e, fila, "Tendencia decreciente:",          res.getInsumosTendenciaDecreciente());

        autosizeColumnas(sheet, cols.length);
    }

    // ── PEDIDOS ────────────────────────────────────────────────────────────

    private static void escribirHojaPedidos(XSSFWorkbook wb, ReporteDTO.Response reporte) {
        Sheet sheet = wb.createSheet("Pedidos");
        Estilos e = new Estilos(wb);

        String[] cols = { "Nº Pedido", "Colegio", "Estado", "Fecha pedido",
                          "Fecha entrega est.", "Días restantes", "% Cumplimiento", "Semáforo" };

        int fila = 0;
        fila = escribirCabecera(sheet, e, fila,
                "REPORTE DE PEDIDOS", cols.length, reporte.getResumen());
        fila = escribirEncabezados(sheet, e, fila, cols);

        boolean alt = false;
        for (ReporteDTO.PedidoItem item : reporte.getPedidos()) {
            Row row = sheet.createRow(fila++);
            CellStyle base = alt ? e.datoGris : e.datoNormal;

            byte[] colorSem;
            switch (nvl(item.getSemaforo())) {
                case "VERDE"     -> colorSem = COL_VERDE;
                case "AMARILLO"  -> colorSem = COL_AMARILLO;
                case "ROJO"      -> colorSem = COL_ROJO;
                case "ENTREGADO" -> colorSem = COL_AZUL;
                default          -> colorSem = COL_GRIS_ENC;   // CANCELADO / SIN_FECHA
            }

            celda(row, 0, nvl(item.getNumeroPedido()),                        base);
            celda(row, 1, nvl(item.getColegio()),                             base);
            celda(row, 2, nvl(item.getEstadoDescripcion()),                   base);
            celda(row, 3, nvl(item.getFechaPedido()),                         base);
            celda(row, 4, nvl(item.getFechaEstimadaEntrega()),                base);
            celdaNumNullable(row, 5, item.getDiasRestantes(),                 base);
            celdaNumNullable(row, 6, item.getPorcentajeCumplimiento(),        base);
            celdaColor(wb, row, 7, nvl(item.getSemaforoDescripcion()),        colorSem);
            alt = !alt;
        }

        fila++;
        ReporteDTO.ResumenResponse res = reporte.getResumen();
        fila = filaResumen(sheet, e, fila, "Total pedidos:",        res.getTotalPedidos());
        fila = filaResumen(sheet, e, fila, "En tiempo (verde):",    res.getPedidosVerdes());
        fila = filaResumen(sheet, e, fila, "Próximos (amarillo):",  res.getPedidosAmarillos());
        fila = filaResumen(sheet, e, fila, "Retrasados (rojo):",    res.getPedidosRojos());
        fila = filaResumen(sheet, e, fila, "Entregados:",           res.getPedidosEntregados());
             filaResumen(sheet, e, fila, "Cancelados:",            res.getPedidosCancelados());

        autosizeColumnas(sheet, cols.length);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS ESTRUCTURALES
    // ════════════════════════════════════════════════════════════════════════

    /** Escribe título + info periodo + espacio. Retorna la siguiente fila libre. */
    private static int escribirCabecera(Sheet sheet, Estilos e, int fila,
            String titulo, int numCols, ReporteDTO.ResumenResponse res) {

        // Título
        Row rowTit = sheet.createRow(fila++);
        rowTit.setHeightInPoints(28);
        Cell cTit = rowTit.createCell(0);
        cTit.setCellValue(titulo);
        cTit.setCellStyle(e.titulo);
        sheet.addMergedRegion(new CellRangeAddress(fila - 1, fila - 1, 0, numCols - 1));

        // Info
        fila = filaInfo(sheet, e, fila,
                "Periodo: " + res.getFechaInicio() + " al " + res.getFechaFin());
        fila = filaInfo(sheet, e, fila,
                "Generado: " + nvl(res.getGeneradoEn()));
        return fila + 1; // espacio
    }

    /** Escribe fila de encabezados. Retorna la siguiente fila libre. */
    private static int escribirEncabezados(Sheet sheet, Estilos e, int fila, String[] cols) {
        Row row = sheet.createRow(fila++);
        row.setHeightInPoints(20);
        for (int i = 0; i < cols.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(e.encabezado);
        }
        return fila;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS DE CELDA
    // ════════════════════════════════════════════════════════════════════════

    private static void celda(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(style);
    }

    private static void celdaNum(Row row, int col, int val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    private static void celdaNumNullable(Row row, int col, Integer val, CellStyle style) {
        Cell c = row.createCell(col);
        if (val != null) c.setCellValue(val);
        else             c.setCellValue("—");
        c.setCellStyle(style);
    }

    private static void celdaDec(Row row, int col, BigDecimal val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

    /** Celda con fondo de color RGB y texto blanco en negrita. */
    private static void celdaColor(XSSFWorkbook wb, Row row, int col, String texto, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFColor color = new XSSFColor(rgb, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        // Texto blanco para fondos oscuros, negro para amarillo
        boolean fondoClaro = rgb == COL_AMARILLO;
        font.setColor(fondoClaro
                ? new XSSFColor(new byte[]{0, 0, 0}, null)
                : new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        style.setFont(font);

        Cell c = row.createCell(col);
        c.setCellValue(texto != null ? texto : "");
        c.setCellStyle(style);
    }

    private static int filaInfo(Sheet sheet, Estilos e, int numFila, String texto) {
        Row row = sheet.createRow(numFila);
        Cell c = row.createCell(0);
        c.setCellValue(texto);
        c.setCellStyle(e.info);
        return numFila + 1;
    }

    private static int filaResumen(Sheet sheet, Estilos e, int numFila,
            String etiqueta, Integer valor) {
        Row row = sheet.createRow(numFila);
        Cell cEt = row.createCell(0);
        cEt.setCellValue(etiqueta);
        cEt.setCellStyle(e.resumen);
        Cell cVal = row.createCell(1);
        if (valor != null) cVal.setCellValue(valor);
        else               cVal.setCellValue("—");
        cVal.setCellStyle(e.resumen);
        return numFila + 1;
    }

    private static void autosizeColumnas(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            // Añadir pequeño padding (POI autosize puede quedar justo)
            int ancho = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(ancho + 512, 20000));
        }
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    // ════════════════════════════════════════════════════════════════════════
    //  ESTILOS REUTILIZABLES
    // ════════════════════════════════════════════════════════════════════════

    /** Contenedor de estilos para evitar crear instancias duplicadas. */
    private static class Estilos {
        final XSSFCellStyle titulo;
        final XSSFCellStyle encabezado;
        final XSSFCellStyle datoNormal;
        final XSSFCellStyle datoGris;
        final XSSFCellStyle resumen;
        final XSSFCellStyle info;

        Estilos(XSSFWorkbook wb) {
            titulo     = crearTitulo(wb);
            encabezado = crearEncabezado(wb);
            datoNormal = crearDato(wb, false);
            datoGris   = crearDato(wb, true);
            resumen    = crearResumen(wb);
            info       = crearInfo(wb);
        }

        private static XSSFCellStyle crearTitulo(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setFontHeightInPoints((short) 14);
            f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
            s.setFont(f);
            s.setFillForegroundColor(new XSSFColor(COL_TITULO, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }

        private static XSSFCellStyle crearEncabezado(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
            s.setFont(f);
            s.setFillForegroundColor(new XSSFColor(COL_GRIS_ENC, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setBorderBottom(BorderStyle.MEDIUM);
            return s;
        }

        private static XSSFCellStyle crearDato(XSSFWorkbook wb, boolean gris) {
            XSSFCellStyle s = wb.createCellStyle();
            if (gris) {
                s.setFillForegroundColor(new XSSFColor(COL_GRIS_ALT, null));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            return s;
        }

        private static XSSFCellStyle crearResumen(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setBold(true);
            s.setFont(f);
            return s;
        }

        private static XSSFCellStyle crearInfo(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            XSSFFont f = wb.createFont();
            f.setItalic(true);
            s.setFont(f);
            return s;
        }
    }
}
