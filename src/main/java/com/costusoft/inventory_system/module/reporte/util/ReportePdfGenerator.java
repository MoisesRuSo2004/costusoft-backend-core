package com.costusoft.inventory_system.module.reporte.util;

import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Generador de reportes en formato PDF usando OpenPDF (fork libre de iText).
 *
 * Estructura del PDF:
 * - Titulo y metadata
 * - Datos del filtro aplicado
 * - Tabla de insumos con entradas / salidas / stock
 * - Seccion de resumen con totales
 */
@Slf4j
public class ReportePdfGenerator {

    private static final Color COLOR_HEADER = new Color(52, 73, 94); // azul oscuro
    private static final Color COLOR_ALERTA = new Color(231, 76, 60); // rojo stock bajo
    private static final Color COLOR_GRIS = new Color(236, 240, 241); // gris claro filas pares
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReportePdfGenerator() {
    }

    public static ByteArrayInputStream generar(ReporteDTO.Response reporte) {
        Document document = new Document(PageSize.A4, 40, 40, 60, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // ── Titulo ────────────────────────────────────────────────────
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);

            PdfPCell celdaTitulo = new PdfPCell(
                    new Phrase("REPORTE DE INVENTARIO", fontTitulo));
            celdaTitulo.setBackgroundColor(COLOR_HEADER);
            celdaTitulo.setPadding(12);
            celdaTitulo.setBorder(Rectangle.NO_BORDER);
            celdaTitulo.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.addCell(celdaTitulo);
            document.add(header);

            // ── Info del filtro ────────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            Font fontInfo = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            ReporteDTO.ResumenResponse resumen = reporte.getResumen();

            document.add(new Paragraph("Tipo de informe: " + resumen.getTipoInforme(), fontInfo));
            document.add(new Paragraph(
                    "Periodo: " + resumen.getFechaInicio() + " al " + resumen.getFechaFin(), fontInfo));
            document.add(new Paragraph(
                    "Generado: " + LocalDate.now().format(FMT), fontInfo));
            document.add(Chunk.NEWLINE);

            // ── Tabla de datos ─────────────────────────────────────────────
            PdfPTable tabla = new PdfPTable(6);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[] { 4f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f });
            tabla.setSpacingBefore(8f);

            // Encabezados
            String[] columnas = { "Insumo", "Unidad", "Entradas", "Salidas", "Stock", "Estado" };
            Font fontEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            Stream.of(columnas).forEach(col -> {
                PdfPCell cell = new PdfPCell(new Phrase(col, fontEncabezado));
                cell.setBackgroundColor(COLOR_HEADER);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(cell);
            });

            // Filas de datos
            Font fontDato = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font fontAlerta = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_ALERTA);

            boolean filaAlterna = false;
            for (ReporteDTO.ItemResponse item : reporte.getItems()) {
                Color bgFila = filaAlterna ? COLOR_GRIS : Color.WHITE;
                Font font = item.isStockBajo() ? fontAlerta : fontDato;

                agregarCelda(tabla, item.getNombreInsumo(), bgFila, font, Element.ALIGN_LEFT);
                agregarCelda(tabla, item.getUnidadMedida(), bgFila, font, Element.ALIGN_CENTER);
                agregarCelda(tabla, String.valueOf(item.getEntradas()), bgFila, font, Element.ALIGN_CENTER);
                agregarCelda(tabla, String.valueOf(item.getSalidas()), bgFila, font, Element.ALIGN_CENTER);
                agregarCelda(tabla, String.valueOf(item.getStockActual()), bgFila, font, Element.ALIGN_CENTER);
                agregarCelda(tabla,
                        item.getStockActual() == 0 ? "CRITICO" : item.isStockBajo() ? "BAJO" : "OK",
                        bgFila, font, Element.ALIGN_CENTER);

                filaAlterna = !filaAlterna;
            }
            document.add(tabla);

            // ── Resumen ────────────────────────────────────────────────────
            document.add(Chunk.NEWLINE);
            Font fontResumenTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            document.add(new Paragraph("RESUMEN", fontResumenTitulo));
            document.add(Chunk.NEWLINE);

            PdfPTable tablaResumen = new PdfPTable(2);
            tablaResumen.setWidthPercentage(50);
            tablaResumen.setHorizontalAlignment(Element.ALIGN_LEFT);

            Font fontResumen = FontFactory.getFont(FontFactory.HELVETICA, 9);
            agregarFilaResumen(tablaResumen, "Total insumos:", String.valueOf(resumen.getTotalInsumos()), fontResumen);
            agregarFilaResumen(tablaResumen, "Total entradas:", String.valueOf(resumen.getTotalEntradas()),
                    fontResumen);
            agregarFilaResumen(tablaResumen, "Total salidas:", String.valueOf(resumen.getTotalSalidas()), fontResumen);
            agregarFilaResumen(tablaResumen, "Insumos stock bajo:", String.valueOf(resumen.getInsumosConStockBajo()),
                    fontResumen);
            agregarFilaResumen(tablaResumen, "Insumos sin stock:", String.valueOf(resumen.getInsumosConStockCero()),
                    fontResumen);

            document.add(tablaResumen);
            document.close();

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private static void agregarCelda(PdfPTable tabla, String texto,
            Color bg, Font font, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setHorizontalAlignment(alineacion);
        tabla.addCell(cell);
    }

    private static void agregarFilaResumen(PdfPTable tabla,
            String etiqueta, String valor, Font font) {
        PdfPCell cEtiqueta = new PdfPCell(new Phrase(etiqueta, font));
        cEtiqueta.setBorder(Rectangle.NO_BORDER);
        cEtiqueta.setPadding(3);
        tabla.addCell(cEtiqueta);

        PdfPCell cValor = new PdfPCell(new Phrase(valor, font));
        cValor.setBorder(Rectangle.NO_BORDER);
        cValor.setPadding(3);
        tabla.addCell(cValor);
    }
}
