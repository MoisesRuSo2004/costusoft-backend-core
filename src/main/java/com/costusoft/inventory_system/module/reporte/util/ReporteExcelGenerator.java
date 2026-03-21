package com.costusoft.inventory_system.module.reporte.util;

import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

/**
 * Generador de reportes en formato Excel (.xlsx) usando Apache POI.
 *
 * Estructura del Excel:
 * - Fila de titulo con merge
 * - Info del filtro aplicado
 * - Tabla de datos con encabezados coloreados
 * - Seccion de resumen con totales
 * - Autosize en todas las columnas
 */
@Slf4j
public class ReporteExcelGenerator {

    private ReporteExcelGenerator() {
    }

    public static ByteArrayInputStream generar(ReporteDTO.Response reporte) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reporte de Inventario");

            // ── Estilos ───────────────────────────────────────────────────
            CellStyle estTitulo = crearEstiloTitulo(wb);
            CellStyle estEncabezado = crearEstiloEncabezado(wb);
            CellStyle estDatoNormal = crearEstiloDato(wb, false, false);
            CellStyle estDatoAlerta = crearEstiloDato(wb, true, false);
            CellStyle estDatoGris = crearEstiloDato(wb, false, true);
            CellStyle estResumen = crearEstiloResumen(wb);

            int fila = 0;

            // ── Titulo ────────────────────────────────────────────────────
            Row rowTitulo = sheet.createRow(fila++);
            rowTitulo.setHeightInPoints(28);
            Cell cTitulo = rowTitulo.createCell(0);
            cTitulo.setCellValue("REPORTE DE INVENTARIO — " +
                    reporte.getResumen().getTipoInforme());
            cTitulo.setCellStyle(estTitulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // ── Info filtro ───────────────────────────────────────────────
            crearFilaInfo(sheet, fila++, wb,
                    "Periodo: " + reporte.getResumen().getFechaInicio() +
                            " al " + reporte.getResumen().getFechaFin());
            crearFilaInfo(sheet, fila++, wb,
                    "Generado: " + LocalDate.now());
            fila++; // espacio

            // ── Encabezados tabla ─────────────────────────────────────────
            String[] columnas = { "Insumo", "Unidad", "Entradas", "Salidas", "Stock actual", "Estado" };
            Row rowEnc = sheet.createRow(fila++);
            rowEnc.setHeightInPoints(20);
            for (int i = 0; i < columnas.length; i++) {
                Cell c = rowEnc.createCell(i);
                c.setCellValue(columnas[i]);
                c.setCellStyle(estEncabezado);
            }

            // ── Filas de datos ────────────────────────────────────────────
            boolean alterna = false;
            for (ReporteDTO.ItemResponse item : reporte.getItems()) {
                Row row = sheet.createRow(fila++);
                CellStyle estFila = item.isStockBajo() ? estDatoAlerta
                        : alterna ? estDatoGris : estDatoNormal;

                row.createCell(0).setCellValue(item.getNombreInsumo());
                row.createCell(1).setCellValue(item.getUnidadMedida());
                row.createCell(2).setCellValue(item.getEntradas());
                row.createCell(3).setCellValue(item.getSalidas());
                row.createCell(4).setCellValue(item.getStockActual());
                row.createCell(5).setCellValue(
                        item.getStockActual() == 0 ? "CRITICO"
                                : item.isStockBajo() ? "BAJO" : "OK");

                for (int i = 0; i <= 5; i++) {
                    row.getCell(i).setCellStyle(estFila);
                }
                alterna = !alterna;
            }

            // ── Resumen ───────────────────────────────────────────────────
            fila++;
            ReporteDTO.ResumenResponse res = reporte.getResumen();
            crearFilaResumen(sheet, fila++, estResumen, "Total insumos:", res.getTotalInsumos());
            crearFilaResumen(sheet, fila++, estResumen, "Total entradas:", res.getTotalEntradas());
            crearFilaResumen(sheet, fila++, estResumen, "Total salidas:", res.getTotalSalidas());
            crearFilaResumen(sheet, fila++, estResumen, "Insumos con stock bajo:", res.getInsumosConStockBajo());
            crearFilaResumen(sheet, fila, estResumen, "Insumos sin stock:", res.getInsumosConStockCero());

            // ── Autosize columnas ─────────────────────────────────────────
            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            log.error("Error generando Excel: {}", e.getMessage(), e);
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    // ── Helpers de estilo ────────────────────────────────────────────────

    private static CellStyle crearEstiloTitulo(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle crearEstiloEncabezado(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private static CellStyle crearEstiloDato(Workbook wb, boolean alerta, boolean gris) {
        CellStyle s = wb.createCellStyle();
        if (alerta) {
            Font f = wb.createFont();
            f.setBold(true);
            f.setColor(IndexedColors.RED.getIndex());
            s.setFont(f);
        }
        if (gris) {
            s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private static CellStyle crearEstiloResumen(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private static void crearFilaInfo(Sheet sheet, int numFila, Workbook wb, String texto) {
        Row row = sheet.createRow(numFila);
        Cell c = row.createCell(0);
        c.setCellValue(texto);
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        s.setFont(f);
        c.setCellStyle(s);
    }

    private static void crearFilaResumen(Sheet sheet, int numFila,
            CellStyle estilo, String etiqueta, int valor) {
        Row row = sheet.createRow(numFila);
        Cell cEt = row.createCell(0);
        cEt.setCellValue(etiqueta);
        cEt.setCellStyle(estilo);
        Cell cVal = row.createCell(1);
        cVal.setCellValue(valor);
        cVal.setCellStyle(estilo);
    }
}
