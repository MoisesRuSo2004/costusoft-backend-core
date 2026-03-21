package com.costusoft.inventory_system.module.reporte.service;

import com.costusoft.inventory_system.entity.DetalleEntrada;
import com.costusoft.inventory_system.entity.DetalleSalida;
import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.repo.EntradaRepository;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.SalidaRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import com.costusoft.inventory_system.module.reporte.util.ReporteExcelGenerator;
import com.costusoft.inventory_system.module.reporte.util.ReportePdfGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private final InsumoRepository insumoRepository;
    private final EntradaRepository entradaRepository;
    private final SalidaRepository salidaRepository;

    @Override
    public ReporteDTO.Response generarReporte(ReporteDTO.FiltroRequest filtro) {
        LocalDate inicio = parsearFecha(filtro.getFechaInicio());
        LocalDate fin = parsearFecha(filtro.getFechaFin());

        validarRangoFechas(inicio, fin);

        // ── Calcular entradas por insumo en el rango ──────────────────────
        Map<Long, Integer> entradasPorInsumo = entradaRepository
                .findByFechaBetween(inicio, fin)
                .stream()
                .flatMap(e -> e.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleEntrada::getCantidad)));

        // ── Calcular salidas por insumo en el rango ───────────────────────
        Map<Long, Integer> salidasPorInsumo = salidaRepository
                .findByFechaBetween(inicio, fin)
                .stream()
                .flatMap(s -> s.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleSalida::getCantidad)));

        // ── Obtener todos los insumos involucrados ────────────────────────
        List<Insumo> insumos = resolverInsumosSegunTipo(
                filtro.getTipoInforme(), entradasPorInsumo, salidasPorInsumo);

        // ── Construir items del reporte ────────────────────────────────────
        List<ReporteDTO.ItemResponse> items = insumos.stream()
                .map(insumo -> ReporteDTO.ItemResponse.builder()
                        .insumoId(insumo.getId())
                        .nombreInsumo(insumo.getNombre())
                        .unidadMedida(insumo.getUnidadMedida())
                        .entradas(entradasPorInsumo.getOrDefault(insumo.getId(), 0))
                        .salidas(salidasPorInsumo.getOrDefault(insumo.getId(), 0))
                        .stockActual(insumo.getStock())
                        .stockMinimo(insumo.getStockMinimo())
                        .stockBajo(insumo.tieneStockBajo())
                        .build())
                .sorted(Comparator.comparing(ReporteDTO.ItemResponse::getNombreInsumo))
                .toList();

        // ── Calcular resumen ──────────────────────────────────────────────
        int totalEntradas = items.stream().mapToInt(ReporteDTO.ItemResponse::getEntradas).sum();
        int totalSalidas = items.stream().mapToInt(ReporteDTO.ItemResponse::getSalidas).sum();
        int stockBajo = (int) items.stream().filter(ReporteDTO.ItemResponse::isStockBajo).count();
        int stockCero = (int) items.stream()
                .filter(i -> i.getStockActual() == 0).count();

        ReporteDTO.ResumenResponse resumen = ReporteDTO.ResumenResponse.builder()
                .totalInsumos(items.size())
                .totalEntradas(totalEntradas)
                .totalSalidas(totalSalidas)
                .insumosConStockBajo(stockBajo)
                .insumosConStockCero(stockCero)
                .fechaInicio(filtro.getFechaInicio())
                .fechaFin(filtro.getFechaFin())
                .tipoInforme(filtro.getTipoInforme())
                .build();

        log.info("Reporte generado — tipo: {} | items: {} | rango: {} a {}",
                filtro.getTipoInforme(), items.size(), inicio, fin);

        return ReporteDTO.Response.builder()
                .items(items)
                .resumen(resumen)
                .build();
    }

    @Override
    public ByteArrayInputStream exportarPdf(ReporteDTO.FiltroRequest filtro) {
        ReporteDTO.Response reporte = generarReporte(filtro);
        return ReportePdfGenerator.generar(reporte);
    }

    @Override
    public ByteArrayInputStream exportarExcel(ReporteDTO.FiltroRequest filtro) {
        ReporteDTO.Response reporte = generarReporte(filtro);
        return ReporteExcelGenerator.generar(reporte);
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private List<Insumo> resolverInsumosSegunTipo(
            String tipo,
            Map<Long, Integer> entradasPorInsumo,
            Map<Long, Integer> salidasPorInsumo) {

        return switch (tipo.toUpperCase()) {
            case "ENTRADAS" -> insumoRepository.findAllById(entradasPorInsumo.keySet());
            case "SALIDAS" -> insumoRepository.findAllById(salidasPorInsumo.keySet());
            case "STOCK_BAJO" -> insumoRepository.findInsumosConStockBajo();
            case "GENERAL" -> {
                Set<Long> ids = new HashSet<>();
                ids.addAll(entradasPorInsumo.keySet());
                ids.addAll(salidasPorInsumo.keySet());
                yield ids.isEmpty()
                        ? insumoRepository.findAll()
                        : insumoRepository.findAllById(ids);
            }
            default -> throw new BusinessException(
                    "Tipo de informe invalido: '" + tipo +
                            "'. Valores aceptados: GENERAL, ENTRADAS, SALIDAS, STOCK_BAJO");
        };
    }

    private LocalDate parsearFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Formato de fecha invalido: '" + fecha +
                    "'. Use el formato yyyy-MM-dd");
        }
    }

    private void validarRangoFechas(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new BusinessException(
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        if (inicio.plusYears(1).isBefore(fin)) {
            throw new BusinessException(
                    "El rango del reporte no puede superar 1 año.");
        }
    }
}
