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
 * Tipos soportados:
 *   GENERAL / ENTRADAS / SALIDAS / STOCK_BAJO  → tabla de inventario
 *   ROTACION                                   → tabla con índice, cobertura y categoría
 *   CONSUMO_PROMEDIO                           → tabla con tasas y tendencia
 *   PEDIDOS                                    → tabla con semáforo visual
 */
@Slf4j
public class ReportePdfGenerator {

    // ── Paleta de colores ────────────────────────────────────────────────
    private static final Color C_HEADER   = new Color(52,  73,  94);   // azul oscuro
    private static final Color C_ALERTA   = new Color(231, 76,  60);   // rojo crítico
    private static final Color C_WARN     = new Color(211, 84,  0);    // naranja bajo
    private static final Color C_OK       = new Color(39,  174, 96);   // verde OK
    private static final Color C_GRIS     = new Color(236, 240, 241);  // fila par
    private static final Color C_VERDE    = new Color(39,  174, 96);
    private static final Color C_AMARILLO = new Color(241, 196, 15);
    private static final Color C_ROJO     = new Color(231, 76,  60);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReportePdfGenerator() { }

    // ══════════════════════════════════════════════════════════════════════
    //  PUNTO DE ENTRADA
    // ══════════════════════════════════════════════════════════════════════

    public static ByteArrayInputStream generar(ReporteDTO.Response reporte) {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 36); // horizontal para más columnas
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            String tipo = reporte.getResumen().getTipoInforme();

            // Título
            escribirTitulo(doc, tipo);
            escribirInfoFiltro(doc, reporte.getResumen());

            // Tabla según tipo
            switch (tipo) {
                case "ROTACION"         -> escribirTablaRotacion(doc, reporte);
                case "CONSUMO_PROMEDIO" -> escribirTablaConsumo(doc, reporte);
                case "PEDIDOS"          -> escribirTablaPedidos(doc, reporte);
                default                 -> escribirTablaInventario(doc, reporte);
            }

            // Resumen
            escribirResumen(doc, reporte.getResumen(), tipo);

            doc.close();
        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SECCIONES COMUNES
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTitulo(Document doc, String tipo) throws DocumentException {
        String titulo = switch (tipo) {
            case "ROTACION"         -> "REPORTE DE ROTACIÓN DE INVENTARIO";
            case "CONSUMO_PROMEDIO" -> "REPORTE DE CONSUMO PROMEDIO";
            case "PEDIDOS"          -> "REPORTE DE PEDIDOS";
            case "STOCK_BAJO"       -> "REPORTE DE STOCK BAJO / CRÍTICO";
            case "ENTRADAS"         -> "REPORTE DE ENTRADAS";
            case "SALIDAS"          -> "REPORTE DE SALIDAS";
            default                 -> "REPORTE GENERAL DE INVENTARIO";
        };

        Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.WHITE);
        PdfPTable tblTitulo = new PdfPTable(1);
        tblTitulo.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell(new Phrase(titulo, fTitulo));
        celda.setBackgroundColor(C_HEADER);
        celda.setPadding(12);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tblTitulo.addCell(celda);
        doc.add(tblTitulo);
        doc.add(Chunk.NEWLINE);
    }

    private static void escribirInfoFiltro(Document doc,
                                            ReporteDTO.ResumenResponse res) throws DocumentException {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY);
        doc.add(new Paragraph("Periodo: " + res.getFechaInicio() + " al " + res.getFechaFin(), f));
        doc.add(new Paragraph("Generado: " + LocalDate.now().format(FMT), f));
        doc.add(Chunk.NEWLINE);
    }

    private static void escribirResumen(Document doc,
                                         ReporteDTO.ResumenResponse res,
                                         String tipo) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font fTit = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        doc.add(new Paragraph("RESUMEN", fTit));
        doc.add(Chunk.NEWLINE);

        PdfPTable tbl = new PdfPTable(2);
        tbl.setWidthPercentage(45);
        tbl.setHorizontalAlignment(Element.ALIGN_LEFT);
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 9);

        switch (tipo) {
            case "ROTACION" -> {
                fila(tbl, "Total insumos analizados:", str(res.getTotalInsumos()), f);
                fila(tbl, "Alta rotación (≥10 u/mes):", str(res.getInsumosAltaRotacion()), f);
                fila(tbl, "Stock muerto (sin movimiento):", str(res.getInsumosStockMuerto()), f);
            }
            case "CONSUMO_PROMEDIO" -> {
                fila(tbl, "Insumos con consumo:", str(res.getTotalInsumos()), f);
                fila(tbl, "Tendencia creciente:", str(res.getInsumosTendenciaCreciente()), f);
                fila(tbl, "Tendencia decreciente:", str(res.getInsumosTendenciaDecreciente()), f);
            }
            case "PEDIDOS" -> {
                fila(tbl, "Total pedidos:", str(res.getTotalPedidos()), f);
                fila(tbl, "🟢 A tiempo:", str(res.getPedidosVerdes()), f);
                fila(tbl, "🟡 Próximos:", str(res.getPedidosAmarillos()), f);
                fila(tbl, "🔴 Retrasados:", str(res.getPedidosRojos()), f);
                fila(tbl, "Entregados:", str(res.getPedidosEntregados()), f);
                fila(tbl, "Cancelados:", str(res.getPedidosCancelados()), f);
            }
            default -> {
                fila(tbl, "Total insumos:", str(res.getTotalInsumos()), f);
                fila(tbl, "Total entradas:", str(res.getTotalEntradas()), f);
                fila(tbl, "Total salidas:", str(res.getTotalSalidas()), f);
                fila(tbl, "Stock bajo:", str(res.getInsumosConStockBajo()), f);
                fila(tbl, "Sin stock:", str(res.getInsumosConStockCero()), f);
            }
        }

        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLA — INVENTARIO (GENERAL/ENTRADAS/SALIDAS/STOCK_BAJO)
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTablaInventario(Document doc,
                                                 ReporteDTO.Response r) throws DocumentException {
        if (r.getItems() == null || r.getItems().isEmpty()) {
            doc.add(new Paragraph("No hay datos para el periodo seleccionado.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));
            return;
        }

        PdfPTable tbl = new PdfPTable(7);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{3.5f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f});
        tbl.setSpacingBefore(6f);

        encabezados(tbl, "Insumo", "Tipo", "Unidad", "Entradas", "Salidas", "Stock", "Estado");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font fA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, C_ALERTA);
        boolean alt = false;

        for (ReporteDTO.ItemResponse item : r.getItems()) {
            Color bg   = alt ? C_GRIS : Color.WHITE;
            Font  font = item.isStockCero() ? fA : (item.isStockBajo() ? fA : fD);

            String estado = item.isStockCero() ? "CRITICO"
                           : item.isStockBajo() ? "BAJO" : "OK";
            Color cEstado = item.isStockCero() ? C_ALERTA
                           : item.isStockBajo() ? C_WARN : C_OK;

            celda(tbl, item.getNombreInsumo(),        bg, fD, Element.ALIGN_LEFT);
            celda(tbl, nulo(item.getTipo()),           bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getUnidadMedida(),         bg, fD, Element.ALIGN_CENTER);
            celda(tbl, String.valueOf(item.getEntradas()), bg, fD, Element.ALIGN_CENTER);
            celda(tbl, String.valueOf(item.getSalidas()),  bg, fD, Element.ALIGN_CENTER);
            celda(tbl, String.valueOf(item.getStockActual()), bg, font, Element.ALIGN_CENTER);
            celdaColor(tbl, estado, bg, cEstado);

            alt = !alt;
        }
        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLA — ROTACIÓN
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTablaRotacion(Document doc,
                                               ReporteDTO.Response r) throws DocumentException {
        if (r.getRotacion() == null || r.getRotacion().isEmpty()) {
            doc.add(new Paragraph("No hay datos de rotación para el periodo seleccionado.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));
            return;
        }

        PdfPTable tbl = new PdfPTable(7);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{3f, 1f, 1.2f, 1.3f, 1.5f, 1.5f, 2f});
        tbl.setSpacingBefore(6f);

        encabezados(tbl, "Insumo", "Stock", "Salidas", "Índice Rot.", "Días Cobertura", "Categoría", "Observación");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;

        for (ReporteDTO.RotacionItem item : r.getRotacion()) {
            Color bg = alt ? C_GRIS : Color.WHITE;

            Color cCat = switch (item.getCategoriaRotacion()) {
                case "Alta rotación"  -> C_OK;
                case "Media rotación" -> new Color(52, 152, 219); // azul
                case "Sin movimiento" -> C_ALERTA;
                default               -> C_WARN;
            };

            celda(tbl, item.getNombreInsumo(),                        bg, fD, Element.ALIGN_LEFT);
            celda(tbl, String.valueOf(item.getStockActual()),          bg, fD, Element.ALIGN_CENTER);
            celda(tbl, String.valueOf(item.getTotalSalidas()),         bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getIndiceRotacion() + " u/mes",           bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getDiasCobertura() != null
                    ? item.getDiasCobertura() + " días" : "∞",        bg, fD, Element.ALIGN_CENTER);
            celdaColor(tbl, item.getCategoriaRotacion(),               bg, cCat);
            celda(tbl, item.isStockMuerto() ? "⚠ Stock muerto" : "",  bg, fD, Element.ALIGN_CENTER);

            alt = !alt;
        }
        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLA — CONSUMO PROMEDIO
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTablaConsumo(Document doc,
                                              ReporteDTO.Response r) throws DocumentException {
        if (r.getConsumo() == null || r.getConsumo().isEmpty()) {
            doc.add(new Paragraph("No hay consumo registrado para el periodo seleccionado.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));
            return;
        }

        PdfPTable tbl = new PdfPTable(8);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{3f, 1f, 1.2f, 1.3f, 1.3f, 1.3f, 1.5f, 1.5f});
        tbl.setSpacingBefore(6f);

        encabezados(tbl, "Insumo", "Stock", "Total Consumo",
                "Diario", "Semanal", "Mensual", "Tendencia", "Días Cobertura");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;

        for (ReporteDTO.ConsumoItem item : r.getConsumo()) {
            Color bg = alt ? C_GRIS : Color.WHITE;

            Color cTend = switch (item.getTendencia()) {
                case "Creciente"   -> C_ALERTA;  // más consumo = posible riesgo de desabasto
                case "Decreciente" -> C_OK;
                case "Estable"     -> new Color(52, 152, 219);
                default            -> Color.GRAY;
            };

            celda(tbl, item.getNombreInsumo(),                             bg, fD, Element.ALIGN_LEFT);
            celda(tbl, String.valueOf(item.getStockActual()),              bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getTotalConsumo() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getConsumoDiario() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getConsumoSemanal() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            celda(tbl, item.getConsumoMensual() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            celdaColor(tbl, item.getTendencia(), bg, cTend);
            celda(tbl, item.getDiasCoberturaEstimados() != null
                    ? item.getDiasCoberturaEstimados() + " días" : "—",   bg, fD, Element.ALIGN_CENTER);

            alt = !alt;
        }
        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TABLA — PEDIDOS
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTablaPedidos(Document doc,
                                              ReporteDTO.Response r) throws DocumentException {
        if (r.getPedidos() == null || r.getPedidos().isEmpty()) {
            doc.add(new Paragraph("No hay pedidos para el periodo seleccionado.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9)));
            return;
        }

        PdfPTable tbl = new PdfPTable(8);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{1.5f, 2.5f, 2f, 1.8f, 1.8f, 1f, 1.5f, 1.5f});
        tbl.setSpacingBefore(6f);

        encabezados(tbl, "# Pedido", "Colegio", "Estado", "Fecha Pedido",
                "F. Entrega", "Días", "Semáforo", "% Cumpl.");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;

        for (ReporteDTO.PedidoItem item : r.getPedidos()) {
            Color bg = alt ? C_GRIS : Color.WHITE;

            Color cSem = switch (item.getSemaforo()) {
                case "VERDE"     -> C_VERDE;
                case "AMARILLO"  -> C_AMARILLO;
                case "ROJO"      -> C_ROJO;
                case "ENTREGADO" -> C_OK;
                default          -> Color.GRAY;
            };

            String diasText = item.getDiasRestantes() != null
                    ? (item.getDiasRestantes() < 0
                            ? "-" + Math.abs(item.getDiasRestantes())
                            : "+" + item.getDiasRestantes())
                    : "—";

            String pctText  = item.getPorcentajeCumplimiento() != null
                    ? item.getPorcentajeCumplimiento() + "%"
                    : "—";

            celda(tbl, nulo(item.getNumeroPedido()),          bg, fD, Element.ALIGN_CENTER);
            celda(tbl, nulo(item.getColegio()),                bg, fD, Element.ALIGN_LEFT);
            celda(tbl, nulo(item.getEstadoDescripcion()),      bg, fD, Element.ALIGN_CENTER);
            celda(tbl, fechaCorta(item.getFechaPedido()),      bg, fD, Element.ALIGN_CENTER);
            celda(tbl, nulo(item.getFechaEstimadaEntrega()),   bg, fD, Element.ALIGN_CENTER);
            celda(tbl, diasText,                               bg, fD, Element.ALIGN_CENTER);
            celdaColor(tbl, item.getSemaforoDescripcion(),     bg, cSem);
            celda(tbl, pctText,                                bg, fD, Element.ALIGN_CENTER);

            alt = !alt;
        }
        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS DE RENDERIZADO
    // ══════════════════════════════════════════════════════════════════════

    private static void encabezados(PdfPTable tbl, String... cols) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Stream.of(cols).forEach(col -> {
            PdfPCell c = new PdfPCell(new Phrase(col, f));
            c.setBackgroundColor(C_HEADER);
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(c);
        });
    }

    private static void celda(PdfPTable tbl, String texto,
                               Color bg, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", f));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        tbl.addCell(c);
    }

    /** Celda con color de texto destacado (para estado/semáforo). */
    private static void celdaColor(PdfPTable tbl, String texto,
                                    Color bg, Color colorTexto) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, colorTexto);
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", f));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        tbl.addCell(c);
    }

    private static void fila(PdfPTable tbl, String etiqueta, String valor, Font f) {
        PdfPCell cE = new PdfPCell(new Phrase(etiqueta, f));
        cE.setBorder(Rectangle.NO_BORDER);
        cE.setPadding(3);
        tbl.addCell(cE);

        Font fV = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        PdfPCell cV = new PdfPCell(new Phrase(valor, fV));
        cV.setBorder(Rectangle.NO_BORDER);
        cV.setPadding(3);
        tbl.addCell(cV);
    }

    private static String nulo(String s)  { return s != null ? s : "—"; }
    private static String str(Integer i)  { return i != null ? String.valueOf(i) : "—"; }

    /** Extrae "yyyy-MM-dd" de un "yyyy-MM-dd HH:mm:ss". */
    private static String fechaCorta(String dt) {
        if (dt == null) return "—";
        return dt.length() >= 10 ? dt.substring(0, 10) : dt;
    }
}
