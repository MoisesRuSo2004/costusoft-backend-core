package com.costusoft.inventory_system.module.dashboard.service;

import com.costusoft.inventory_system.entity.Entrada;
import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.entity.Salida;
import com.costusoft.inventory_system.repo.*;
import com.costusoft.inventory_system.module.dashboard.dto.DashboardDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementacion del servicio de dashboard.
 *
 * Todas las operaciones son readOnly — el dashboard nunca modifica datos.
 * Se agregan metricas de los ultimos 6 meses para los graficos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int ULTIMOS_MESES = 6;
    private static final int ULTIMOS_MOVIMIENTOS = 10;

    private final InsumoRepository insumoRepository;
    private final EntradaRepository entradaRepository;
    private final SalidaRepository salidaRepository;
    private final ColegioRepository colegioRepository;
    private final UniformeRepository uniformeRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public DashboardDTO generarResumen() {
        log.debug("Generando resumen del dashboard");

        // ── Contadores ────────────────────────────────────────────────────
        long totalInsumos = insumoRepository.count();
        long totalEntradas = entradaRepository.count();
        long totalSalidas = salidaRepository.count();
        long totalColegios = colegioRepository.count();
        long totalUniformes = uniformeRepository.count();
        long totalProveedores = proveedorRepository.count();
        long totalUsuarios = usuarioRepository.count();

        // ── Alertas de stock ──────────────────────────────────────────────
        List<Insumo> insumosStockBajo = insumoRepository.findInsumosConStockBajo();
        List<Insumo> insumosStockCero = insumoRepository.findByStock(0);

        List<DashboardDTO.AlertaStockDTO> alertas = insumosStockBajo.stream()
                .map(this::toAlertaDTO)
                .toList();

        // ── Graficos ultimos 6 meses ──────────────────────────────────────
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusMonths(ULTIMOS_MESES).withDayOfMonth(1);

        List<Entrada> entradas = entradaRepository.findByFechaBetween(inicio, hoy);
        List<Salida> salidas = salidaRepository.findByFechaBetween(inicio, hoy);

        Map<String, Long> entradasPorMes = agruparPorMes(
                entradas.stream().map(Entrada::getFecha).toList());
        Map<String, Long> salidasPorMes = agruparPorMes(
                salidas.stream().map(Salida::getFecha).toList());

        // Rellenar meses sin movimientos con cero (para graficos continuos)
        rellenarMesesVacios(entradasPorMes, inicio, hoy);
        rellenarMesesVacios(salidasPorMes, inicio, hoy);

        // ── Ultimos movimientos ───────────────────────────────────────────
        List<DashboardDTO.MovimientoDTO> movimientos = new ArrayList<>();

        entradaRepository
                .findAllByOrderByFechaDesc(PageRequest.of(0, ULTIMOS_MOVIMIENTOS,
                        Sort.by("fecha").descending()))
                .getContent()
                .forEach(e -> movimientos.add(DashboardDTO.MovimientoDTO.builder()
                        .tipo("ENTRADA")
                        .descripcion(e.getDescripcion() != null
                                ? e.getDescripcion()
                                : "Entrada de insumos")
                        .fecha(e.getFecha() != null ? e.getFecha().toString() : "")
                        .totalItems(e.getDetalles() != null ? e.getDetalles().size() : 0)
                        .build()));

        salidaRepository
                .findAllByOrderByFechaDesc(PageRequest.of(0, ULTIMOS_MOVIMIENTOS,
                        Sort.by("fecha").descending()))
                .getContent()
                .forEach(s -> movimientos.add(DashboardDTO.MovimientoDTO.builder()
                        .tipo("SALIDA")
                        .descripcion(s.getDescripcion() != null
                                ? s.getDescripcion()
                                : "Salida de insumos")
                        .fecha(s.getFecha() != null ? s.getFecha().toString() : "")
                        .totalItems(s.getDetalles() != null ? s.getDetalles().size() : 0)
                        .build()));

        // Ordenar movimientos por fecha desc y tomar los 10 mas recientes
        movimientos.sort(Comparator.comparing(DashboardDTO.MovimientoDTO::getFecha).reversed());
        List<DashboardDTO.MovimientoDTO> ultimosMovimientos = movimientos.stream()
                .limit(ULTIMOS_MOVIMIENTOS)
                .toList();

        // ── Uniformes por colegio ─────────────────────────────────────────
        Map<String, Long> uniformesPorColegio = colegioRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getNombre(),
                        c -> (long) uniformeRepository.findByColegioId(c.getId()).size(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        return DashboardDTO.builder()
                .totalInsumos(totalInsumos)
                .totalEntradas(totalEntradas)
                .totalSalidas(totalSalidas)
                .totalColegios(totalColegios)
                .totalUniformes(totalUniformes)
                .totalProveedores(totalProveedores)
                .totalUsuarios(totalUsuarios)
                .insumosConStockBajo(insumosStockBajo.size())
                .insumosConStockCero(insumosStockCero.size())
                .alertasStock(alertas)
                .entradasPorMes(entradasPorMes)
                .salidasPorMes(salidasPorMes)
                .ultimosMovimientos(ultimosMovimientos)
                .uniformesPorColegio(uniformesPorColegio)
                .build();
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private DashboardDTO.AlertaStockDTO toAlertaDTO(Insumo insumo) {
        // Nivel de riesgo calculado dinamicamente
        String nivelRiesgo;
        if (insumo.getStock() == 0) {
            nivelRiesgo = "CRITICO";
        } else if (insumo.getStock() <= insumo.getStockMinimo() / 2) {
            nivelRiesgo = "ALTO";
        } else if (insumo.getStock() <= insumo.getStockMinimo()) {
            nivelRiesgo = "MEDIO";
        } else {
            nivelRiesgo = "BAJO";
        }

        return DashboardDTO.AlertaStockDTO.builder()
                .id(insumo.getId())
                .nombre(insumo.getNombre())
                .stockActual(insumo.getStock())
                .stockMinimo(insumo.getStockMinimo())
                .unidadMedida(insumo.getUnidadMedida())
                .nivelRiesgo(nivelRiesgo)
                .build();
    }

    /**
     * Agrupa una lista de fechas por mes y cuenta ocurrencias.
     * Retorna un mapa ordenado: "2024-01" -> 5
     */
    private Map<String, Long> agruparPorMes(List<LocalDate> fechas) {
        return fechas.stream()
                .collect(Collectors.groupingBy(
                        f -> f.format(MES_FMT),
                        TreeMap::new,
                        Collectors.counting()));
    }

    /**
     * Rellena los meses sin movimientos con cero para que el grafico
     * del frontend no tenga huecos en el eje X.
     */
    private void rellenarMesesVacios(
            Map<String, Long> mapa,
            LocalDate inicio,
            LocalDate fin) {

        LocalDate cursor = inicio.withDayOfMonth(1);
        while (!cursor.isAfter(fin)) {
            mapa.putIfAbsent(cursor.format(MES_FMT), 0L);
            cursor = cursor.plusMonths(1);
        }
    }
}
