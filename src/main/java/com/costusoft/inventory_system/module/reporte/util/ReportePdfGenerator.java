package com.costusoft.inventory_system.module.reporte.util;

import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Slf4j
public class ReportePdfGenerator {

    // ── Paleta ───────────────────────────────────────────────────────────
    private static final Color C_BLUE_DARK = new Color(11, 61, 145);
    private static final Color C_GOLD = new Color(217, 143, 20);
    private static final Color C_GREY_TEXT = new Color(120, 130, 148);
    private static final Color C_TH_BG = new Color(37, 99, 235);
    private static final Color C_ROW_ALT = new Color(249, 250, 251);
    private static final Color C_ALERTA = new Color(220, 38, 38);
    private static final Color C_WARN = new Color(217, 119, 6);
    private static final Color C_OK = new Color(5, 150, 105);
    private static final Color C_BLUE_MID = new Color(52, 152, 219);

    // ── Medidas (puntos PDF: 1pt = 1/72") ────────────────────────────────
    private static final float PAGE_W = PageSize.A4.rotate().getWidth(); // 841.89
    private static final float PAGE_H = PageSize.A4.rotate().getHeight(); // 595.28
    private static final float MARGIN_H = 36f;
    private static final float HEADER_H = 91f; // ~32 mm
    private static final float FOOTER_H = 40f; // ~14 mm
    private static final float LOGO_W = 85f; // ~30 mm
    private static final float SEP_X = MARGIN_H + LOGO_W + 11f;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReportePdfGenerator() {
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUNTO DE ENTRADA
    // ══════════════════════════════════════════════════════════════════════

    public static ByteArrayInputStream generar(ReporteDTO.Response reporte) {
        Document doc = new Document(
                PageSize.A4.rotate(),
                MARGIN_H, MARGIN_H,
                HEADER_H + 10, // top margin = header + gap
                FOOTER_H + 6 // bottom margin = footer + gap
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new HeaderFooterEvent(reporte.getResumen()));
            doc.open();

            String tipo = reporte.getResumen().getTipoInforme();

            escribirTituloSeccion(doc, tipo);
            escribirInfoFiltro(doc, reporte.getResumen());

            switch (tipo) {
                case "ROTACION" -> escribirTablaRotacion(doc, reporte);
                case "CONSUMO_PROMEDIO" -> escribirTablaConsumo(doc, reporte);
                case "PEDIDOS" -> escribirTablaPedidos(doc, reporte);
                default -> escribirTablaInventario(doc, reporte);
            }

            escribirResumen(doc, reporte.getResumen(), tipo);
            doc.close();

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ══════════════════════════════════════════════════════════════════════
    // PAGE EVENT — header y footer en cada pagina
    // ══════════════════════════════════════════════════════════════════════

    private static class HeaderFooterEvent extends PdfPageEventHelper {

        private final ReporteDTO.ResumenResponse resumen;
        private Image logoImg;

        HeaderFooterEvent(ReporteDTO.ResumenResponse resumen) {
            this.resumen = resumen;
            try (InputStream is = ReportePdfGenerator.class
                    .getResourceAsStream("/img/logo.png")) {
                if (is != null) {
                    logoImg = Image.getInstance(is.readAllBytes());
                    logoImg.scaleToFit(LOGO_W, HEADER_H - 14);
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo: {}", e.getMessage());
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            dibujarHeader(cb, writer.getPageNumber());
            dibujarFooter(cb);
        }

        private void dibujarHeader(PdfContentByte cb, int pageNum) {
            float top = PAGE_H;

            // ── fondo azul ────────────────────────────────────────────────
            cb.setColorFill(C_BLUE_DARK);
            cb.rectangle(0, top - HEADER_H, PAGE_W, HEADER_H);
            cb.fill();

            // ── franja dorada inferior ────────────────────────────────────
            cb.setColorFill(C_GOLD);
            cb.rectangle(0, top - HEADER_H, PAGE_W, 2.8f);
            cb.fill();

            // ── logo ──────────────────────────────────────────────────────
            if (logoImg != null) {
                try {
                    float lh = logoImg.getScaledHeight();
                    float ly = top - HEADER_H + (HEADER_H - 2.8f - lh) / 2f + 2.8f;
                    logoImg.setAbsolutePosition(MARGIN_H, ly);
                    cb.addImage(logoImg);
                } catch (Exception ex) {
                    log.warn("Error colocando logo: {}", ex.getMessage());
                }
            }

            // ── separador vertical blanco ─────────────────────────────────
            cb.setColorStroke(new Color(255, 255, 255, 180));
            cb.setLineWidth(0.5f);
            cb.moveTo(SEP_X, top - HEADER_H + 8);
            cb.lineTo(SEP_X, top - 7);
            cb.stroke();

            // ── textos del header ─────────────────────────────────────────
            try {
                float txtX = SEP_X + 10;
                BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                BaseFont bfItalic = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252,
                        BaseFont.NOT_EMBEDDED);

                cb.beginText();

                cb.setColorFill(Color.WHITE);
                cb.setFontAndSize(bfBold, 15);
                cb.showTextAligned(Element.ALIGN_LEFT, "CostuSoft Control", txtX, top - 32, 0);

                cb.setColorFill(new Color(200, 215, 240));
                cb.setFontAndSize(bfNormal, 8);
                cb.showTextAligned(Element.ALIGN_LEFT,
                        "Taller de Confecciones La Senora Piedad  -  Cartagena, Colombia",
                        txtX, top - 47, 0);

                cb.setColorFill(new Color(180, 200, 240));
                cb.setFontAndSize(bfItalic, 7.5f);
                cb.showTextAligned(Element.ALIGN_LEFT, tipoLabel(resumen.getTipoInforme()), txtX, top - 62, 0);

                cb.setColorFill(new Color(180, 200, 240));
                cb.setFontAndSize(bfNormal, 7);
                cb.showTextAligned(Element.ALIGN_RIGHT, "Pagina " + pageNum, PAGE_W - MARGIN_H, top - 78, 0);

                cb.endText();
            } catch (Exception e) {
                log.warn("Error textos header: {}", e.getMessage());
            }
        }

        private void dibujarFooter(PdfContentByte cb) {
            float y = FOOTER_H - 2;

            // ── linea dorada separadora ───────────────────────────────────
            cb.setColorStroke(C_GOLD);
            cb.setLineWidth(0.7f);
            cb.moveTo(MARGIN_H, y + 14);
            cb.lineTo(PAGE_W - MARGIN_H, y + 14);
            cb.stroke();

            try {
                BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                BaseFont bfItalic = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252,
                        BaseFont.NOT_EMBEDDED);

                cb.beginText();

                cb.setColorFill(C_GREY_TEXT);
                cb.setFontAndSize(bfNormal, 7);
                cb.showTextAligned(Element.ALIGN_LEFT,
                        "CostuSoft  -  Sistema de Gestion de Inventario y Produccion",
                        MARGIN_H, y, 0);

                cb.setColorFill(C_BLUE_DARK);
                cb.setFontAndSize(bfBold, 7);
                cb.showTextAligned(Element.ALIGN_CENTER,
                        "Generado: " + LocalDate.now().format(FMT),
                        PAGE_W / 2, y, 0);

                cb.setColorFill(C_GREY_TEXT);
                cb.setFontAndSize(bfItalic, 6.5f);
                cb.showTextAligned(Element.ALIGN_RIGHT,
                        "Reporte generado automaticamente  |  Spring Boot + OpenPDF",
                        PAGE_W - MARGIN_H, y, 0);

                cb.endText();
            } catch (Exception e) {
                log.warn("Error footer: {}", e.getMessage());
            }
        }

        private static String tipoLabel(String tipo) {
            return switch (tipo) {
                case "ROTACION" -> "Informe de Rotacion de Inventario";
                case "CONSUMO_PROMEDIO" -> "Informe de Consumo Promedio";
                case "PEDIDOS" -> "Informe de Pedidos";
                case "STOCK_BAJO" -> "Informe de Stock Bajo / Critico";
                case "ENTRADAS" -> "Informe de Entradas";
                case "SALIDAS" -> "Informe de Salidas";
                default -> "Informe General de Inventario";
            };
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECCIONES DEL CUERPO
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTituloSeccion(Document doc, String tipo) throws DocumentException {
        String titulo = switch (tipo) {
            case "ROTACION" -> "ROTACION DE INVENTARIO";
            case "CONSUMO_PROMEDIO" -> "CONSUMO PROMEDIO";
            case "PEDIDOS" -> "PEDIDOS";
            case "STOCK_BAJO" -> "STOCK BAJO / CRITICO";
            case "ENTRADAS" -> "ENTRADAS";
            case "SALIDAS" -> "SALIDAS";
            default -> "INVENTARIO GENERAL";
        };

        Font fTit = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, C_BLUE_DARK);
        Paragraph p = new Paragraph(titulo, fTit);
        p.setSpacingAfter(2f);
        doc.add(p);

        // linea decorativa azul
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        PdfPCell cel = new PdfPCell(new Phrase(""));
        cel.setBackgroundColor(C_BLUE_DARK);
        cel.setFixedHeight(2f);
        cel.setBorder(Rectangle.NO_BORDER);
        linea.addCell(cel);
        doc.add(linea);
        doc.add(new Paragraph(" "));
    }

    private static void escribirInfoFiltro(Document doc,
            ReporteDTO.ResumenResponse res) throws DocumentException {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, C_GREY_TEXT);
        Paragraph p = new Paragraph("Periodo: " + res.getFechaInicio() + "  al  " + res.getFechaFin(), f);
        p.setSpacingAfter(8f);
        doc.add(p);
    }

    private static void escribirResumen(Document doc,
            ReporteDTO.ResumenResponse res,
            String tipo) throws DocumentException {
        doc.add(new Paragraph(" "));

        Font fTit = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, C_BLUE_DARK);
        Paragraph tit = new Paragraph("RESUMEN", fTit);
        tit.setSpacingAfter(4f);
        doc.add(tit);

        PdfPTable tbl = new PdfPTable(2);
        tbl.setWidthPercentage(38);
        tbl.setHorizontalAlignment(Element.ALIGN_LEFT);
        Font fL = FontFactory.getFont(FontFactory.HELVETICA, 8, C_GREY_TEXT);
        Font fV = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_BLUE_DARK);

        switch (tipo) {
            case "ROTACION" -> {
                filaResumen(tbl, "Total insumos analizados", str(res.getTotalInsumos()), fL, fV);
                filaResumen(tbl, "Alta rotacion (>=10 u/mes)", str(res.getInsumosAltaRotacion()), fL, fV);
                filaResumen(tbl, "Stock muerto (sin movimiento)", str(res.getInsumosStockMuerto()), fL, fV);
            }
            case "CONSUMO_PROMEDIO" -> {
                filaResumen(tbl, "Insumos con consumo", str(res.getTotalInsumos()), fL, fV);
                filaResumen(tbl, "Tendencia creciente", str(res.getInsumosTendenciaCreciente()), fL, fV);
                filaResumen(tbl, "Tendencia decreciente", str(res.getInsumosTendenciaDecreciente()), fL, fV);
            }
            case "PEDIDOS" -> {
                filaResumen(tbl, "Total pedidos", str(res.getTotalPedidos()), fL, fV);
                filaResumen(tbl, "A tiempo", str(res.getPedidosVerdes()), fL, fV);
                filaResumen(tbl, "Proximos vencer", str(res.getPedidosAmarillos()), fL, fV);
                filaResumen(tbl, "Retrasados", str(res.getPedidosRojos()), fL, fV);
                filaResumen(tbl, "Entregados", str(res.getPedidosEntregados()), fL, fV);
                filaResumen(tbl, "Cancelados", str(res.getPedidosCancelados()), fL, fV);
            }
            default -> {
                filaResumen(tbl, "Total insumos", str(res.getTotalInsumos()), fL, fV);
                filaResumen(tbl, "Total entradas", str(res.getTotalEntradas()), fL, fV);
                filaResumen(tbl, "Total salidas", str(res.getTotalSalidas()), fL, fV);
                filaResumen(tbl, "Stock bajo", str(res.getInsumosConStockBajo()), fL, fV);
                filaResumen(tbl, "Sin stock", str(res.getInsumosConStockCero()), fL, fV);
            }
        }

        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TABLAS DE DATOS
    // ══════════════════════════════════════════════════════════════════════

    private static void escribirTablaInventario(Document doc,
            ReporteDTO.Response r) throws DocumentException {
        if (r.getItems() == null || r.getItems().isEmpty()) {
            doc.add(vacio());
            return;
        }

        PdfPTable tbl = new PdfPTable(7);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[] { 3.5f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f });
        tbl.setSpacingBefore(4f);

        th(tbl, "Insumo", "Tipo", "Unidad", "Entradas", "Salidas", "Stock", "Estado");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;
        for (ReporteDTO.ItemResponse item : r.getItems()) {
            Color bg = alt ? C_ROW_ALT : Color.WHITE;
            String estado = item.isStockCero() ? "CRITICO" : item.isStockBajo() ? "BAJO" : "OK";
            Color cE = item.isStockCero() ? C_ALERTA : item.isStockBajo() ? C_WARN : C_OK;

            td(tbl, item.getNombreInsumo(), bg, fD, Element.ALIGN_LEFT);
            td(tbl, nulo(item.getTipo()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            td(tbl, String.valueOf(item.getEntradas()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, String.valueOf(item.getSalidas()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, String.valueOf(item.getStockActual()), bg,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8,
                            item.isStockCero() ? C_ALERTA : item.isStockBajo() ? C_WARN : fD.getColor()),
                    Element.ALIGN_CENTER);
            tdColor(tbl, estado, bg, cE);
            alt = !alt;
        }
        doc.add(tbl);
    }

    private static void escribirTablaRotacion(Document doc,
            ReporteDTO.Response r) throws DocumentException {
        if (r.getRotacion() == null || r.getRotacion().isEmpty()) {
            doc.add(vacio());
            return;
        }

        PdfPTable tbl = new PdfPTable(7);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[] { 3f, 1f, 1.2f, 1.3f, 1.5f, 1.5f, 2f });
        tbl.setSpacingBefore(4f);

        th(tbl, "Insumo", "Stock", "Salidas", "Indice Rot.", "Dias Cobertura", "Categoria", "Observacion");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;
        for (ReporteDTO.RotacionItem item : r.getRotacion()) {
            Color bg = alt ? C_ROW_ALT : Color.WHITE;
            Color cCat = switch (item.getCategoriaRotacion()) {
                case "Alta rotacion" -> C_OK;
                case "Media rotacion" -> C_BLUE_MID;
                case "Sin movimiento" -> C_ALERTA;
                default -> C_WARN;
            };

            td(tbl, item.getNombreInsumo(), bg, fD, Element.ALIGN_LEFT);
            td(tbl, String.valueOf(item.getStockActual()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, String.valueOf(item.getTotalSalidas()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getIndiceRotacion() + " u/mes", bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getDiasCobertura() != null
                    ? item.getDiasCobertura() + " dias"
                    : "Inf.", bg, fD, Element.ALIGN_CENTER);
            tdColor(tbl, item.getCategoriaRotacion(), bg, cCat);
            td(tbl, item.isStockMuerto() ? "Stock muerto" : "", bg, fD, Element.ALIGN_CENTER);
            alt = !alt;
        }
        doc.add(tbl);
    }

    private static void escribirTablaConsumo(Document doc,
            ReporteDTO.Response r) throws DocumentException {
        if (r.getConsumo() == null || r.getConsumo().isEmpty()) {
            doc.add(vacio());
            return;
        }

        PdfPTable tbl = new PdfPTable(8);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[] { 3f, 1f, 1.2f, 1.3f, 1.3f, 1.3f, 1.5f, 1.5f });
        tbl.setSpacingBefore(4f);

        th(tbl, "Insumo", "Stock", "Total Consumo",
                "Diario", "Semanal", "Mensual", "Tendencia", "Dias Cobertura");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;
        for (ReporteDTO.ConsumoItem item : r.getConsumo()) {
            Color bg = alt ? C_ROW_ALT : Color.WHITE;
            Color cTend = switch (item.getTendencia()) {
                case "Creciente" -> C_ALERTA;
                case "Decreciente" -> C_OK;
                case "Estable" -> C_BLUE_MID;
                default -> Color.GRAY;
            };

            td(tbl, item.getNombreInsumo(), bg, fD, Element.ALIGN_LEFT);
            td(tbl, String.valueOf(item.getStockActual()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getTotalConsumo() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getConsumoDiario() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getConsumoSemanal() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            td(tbl, item.getConsumoMensual() + " " + item.getUnidadMedida(), bg, fD, Element.ALIGN_CENTER);
            tdColor(tbl, item.getTendencia(), bg, cTend);
            td(tbl, item.getDiasCoberturaEstimados() != null
                    ? item.getDiasCoberturaEstimados() + " dias"
                    : "-", bg, fD, Element.ALIGN_CENTER);
            alt = !alt;
        }
        doc.add(tbl);
    }

    private static void escribirTablaPedidos(Document doc,
            ReporteDTO.Response r) throws DocumentException {
        if (r.getPedidos() == null || r.getPedidos().isEmpty()) {
            doc.add(vacio());
            return;
        }

        PdfPTable tbl = new PdfPTable(8);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[] { 1.5f, 2.5f, 2f, 1.8f, 1.8f, 1f, 1.5f, 1.5f });
        tbl.setSpacingBefore(4f);

        th(tbl, "# Pedido", "Colegio", "Estado", "Fecha Pedido",
                "F. Entrega", "Dias", "Semaforo", "% Cumpl.");

        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 8);
        boolean alt = false;
        for (ReporteDTO.PedidoItem item : r.getPedidos()) {
            Color bg = alt ? C_ROW_ALT : Color.WHITE;
            Color cSem = switch (item.getSemaforo()) {
                case "VERDE" -> C_OK;
                case "AMARILLO" -> C_WARN;
                case "ROJO" -> C_ALERTA;
                case "ENTREGADO" -> C_OK;
                default -> Color.GRAY;
            };

            String diasText = item.getDiasRestantes() != null
                    ? (item.getDiasRestantes() < 0
                            ? "-" + Math.abs(item.getDiasRestantes())
                            : "+" + item.getDiasRestantes())
                    : "-";
            String pctText = item.getPorcentajeCumplimiento() != null
                    ? item.getPorcentajeCumplimiento() + "%"
                    : "-";

            td(tbl, nulo(item.getNumeroPedido()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, nulo(item.getColegio()), bg, fD, Element.ALIGN_LEFT);
            td(tbl, nulo(item.getEstadoDescripcion()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, fechaCorta(item.getFechaPedido()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, nulo(item.getFechaEstimadaEntrega()), bg, fD, Element.ALIGN_CENTER);
            td(tbl, diasText, bg, fD, Element.ALIGN_CENTER);
            tdColor(tbl, item.getSemaforoDescripcion(), bg, cSem);
            td(tbl, pctText, bg, fD, Element.ALIGN_CENTER);
            alt = !alt;
        }
        doc.add(tbl);
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS DE RENDERIZADO
    // ══════════════════════════════════════════════════════════════════════

    private static void th(PdfPTable tbl, String... cols) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Stream.of(cols).forEach(col -> {
            PdfPCell c = new PdfPCell(new Phrase(col, f));
            c.setBackgroundColor(C_TH_BG);
            c.setPadding(5.5f);
            c.setBorder(Rectangle.NO_BORDER);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            tbl.addCell(c);
        });
    }

    private static void td(PdfPTable tbl, String texto, Color bg, Font f, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", f));
        c.setBackgroundColor(bg);
        c.setPadding(4.5f);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        tbl.addCell(c);
    }

    private static void tdColor(PdfPTable tbl, String texto, Color bg, Color colorTexto) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, colorTexto);
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "", f));
        c.setBackgroundColor(bg);
        c.setPadding(4.5f);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        tbl.addCell(c);
    }

    private static void filaResumen(PdfPTable tbl, String etiqueta, String valor,
            Font fLabel, Font fVal) {
        PdfPCell cE = new PdfPCell(new Phrase(etiqueta, fLabel));
        cE.setBorder(Rectangle.NO_BORDER);
        cE.setPadding(3f);
        tbl.addCell(cE);

        PdfPCell cV = new PdfPCell(new Phrase(valor, fVal));
        cV.setBorder(Rectangle.NO_BORDER);
        cV.setPadding(3f);
        tbl.addCell(cV);
    }

    private static Paragraph vacio() {
        return new Paragraph("No hay datos para el periodo seleccionado.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, C_GREY_TEXT));
    }

    private static String nulo(String s) {
        return s != null ? s : "-";
    }

    private static String str(Integer i) {
        return i != null ? String.valueOf(i) : "-";
    }

    private static String fechaCorta(String dt) {
        if (dt == null)
            return "-";
        return dt.length() >= 10 ? dt.substring(0, 10) : dt;
    }
}
