package com.costusoft.inventory_system.module.reporte.service;

import com.costusoft.inventory_system.entity.*;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.module.reporte.dto.ReporteDTO;
import com.costusoft.inventory_system.module.reporte.util.ReporteExcelGenerator;
import com.costusoft.inventory_system.module.reporte.util.ReportePdfGenerator;
import com.costusoft.inventory_system.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de reportes.
 *
 * Tipos disponibles:
 *   GENERAL, ENTRADAS, SALIDAS, STOCK_BAJO
 *   → informe de movimientos de insumos con stock actual
 *
 *   ROTACION
 *   → índice de rotación, días de cobertura y categoría por insumo
 *
 *   CONSUMO_PROMEDIO
 *   → tasa de consumo diario/semanal/mensual con tendencia
 *
 *   PEDIDOS
 *   → semáforo de pedidos (VERDE/AMARILLO/ROJO) por fecha estimada de entrega
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private final InsumoRepository   insumoRepository;
    private final EntradaRepository  entradaRepository;
    private final SalidaRepository   salidaRepository;
    private final PedidoRepository   pedidoRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ══════════════════════════════════════════════════════════════════════
    //  PUNTO DE ENTRADA PRINCIPAL
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public ReporteDTO.Response generarReporte(ReporteDTO.FiltroRequest filtro) {
        LocalDate inicio = parsearFecha(filtro.getFechaInicio());
        LocalDate fin    = parsearFecha(filtro.getFechaFin());
        validarRangoFechas(inicio, fin);

        String tipo = filtro.getTipoInforme().toUpperCase().trim();

        return switch (tipo) {
            case "ROTACION"         -> generarRotacion(filtro, inicio, fin);
            case "CONSUMO_PROMEDIO" -> generarConsumoPromedio(filtro, inicio, fin);
            case "PEDIDOS"          -> generarPedidos(filtro, inicio, fin);
            default                 -> generarInventario(filtro, inicio, fin, tipo);
        };
    }

    @Override
    public ByteArrayInputStream exportarPdf(ReporteDTO.FiltroRequest filtro) {
        return ReportePdfGenerator.generar(generarReporte(filtro));
    }

    @Override
    public ByteArrayInputStream exportarExcel(ReporteDTO.FiltroRequest filtro) {
        return ReporteExcelGenerator.generar(generarReporte(filtro));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INVENTARIO — GENERAL / ENTRADAS / SALIDAS / STOCK_BAJO
    // ══════════════════════════════════════════════════════════════════════

    private ReporteDTO.Response generarInventario(ReporteDTO.FiltroRequest filtro,
                                                   LocalDate inicio, LocalDate fin,
                                                   String tipo) {
        // Agregar entradas por insumo (solo CONFIRMADAS)
        Map<Long, Integer> entradasMap = entradaRepository.findByFechaBetween(inicio, fin)
                .stream()
                .filter(e -> EstadoMovimiento.CONFIRMADA == e.getEstado())
                .flatMap(e -> e.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleEntrada::getCantidad)));

        // Agregar salidas por insumo (solo CONFIRMADAS)
        Map<Long, Integer> salidasMap = salidaRepository.findByFechaBetween(inicio, fin)
                .stream()
                .filter(s -> EstadoMovimiento.CONFIRMADA == s.getEstado())
                .flatMap(s -> s.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleSalida::getCantidad)));

        List<Insumo> insumos = resolverInsumosInventario(tipo, entradasMap, salidasMap);

        List<ReporteDTO.ItemResponse> items = insumos.stream()
                .map(i -> ReporteDTO.ItemResponse.builder()
                        .insumoId(i.getId())
                        .nombreInsumo(i.getNombre())
                        .unidadMedida(i.getUnidadMedida())
                        .tipo(i.getTipo())
                        .entradas(entradasMap.getOrDefault(i.getId(), 0))
                        .salidas(salidasMap.getOrDefault(i.getId(), 0))
                        .stockActual(i.getStock())
                        .stockMinimo(i.getStockMinimo())
                        .stockBajo(i.tieneStockBajo())
                        .stockCero(i.getStock() == 0)
                        .build())
                .sorted(Comparator.comparing(ReporteDTO.ItemResponse::getNombreInsumo))
                .toList();

        int totalE  = items.stream().mapToInt(ReporteDTO.ItemResponse::getEntradas).sum();
        int totalS  = items.stream().mapToInt(ReporteDTO.ItemResponse::getSalidas).sum();
        int bajo    = (int) items.stream().filter(ReporteDTO.ItemResponse::isStockBajo).count();
        int cero    = (int) items.stream().filter(ReporteDTO.ItemResponse::isStockCero).count();

        log.info("Reporte {} generado — {} items | {} → {}", tipo, items.size(), inicio, fin);

        return ReporteDTO.Response.builder()
                .items(items)
                .resumen(ReporteDTO.ResumenResponse.builder()
                        .tipoInforme(tipo)
                        .fechaInicio(filtro.getFechaInicio())
                        .fechaFin(filtro.getFechaFin())
                        .generadoEn(LocalDateTime.now().format(DT_FMT))
                        .totalInsumos(items.size())
                        .totalEntradas(totalE)
                        .totalSalidas(totalS)
                        .insumosConStockBajo(bajo)
                        .insumosConStockCero(cero)
                        .build())
                .build();
    }

    private List<Insumo> resolverInsumosInventario(String tipo,
                                                    Map<Long, Integer> entradas,
                                                    Map<Long, Integer> salidas) {
        return switch (tipo) {
            case "ENTRADAS"   -> insumoRepository.findAllById(entradas.keySet());
            case "SALIDAS"    -> insumoRepository.findAllById(salidas.keySet());
            case "STOCK_BAJO" -> insumoRepository.findInsumosConStockBajo();
            case "GENERAL"    -> {
                Set<Long> ids = new HashSet<>();
                ids.addAll(entradas.keySet());
                ids.addAll(salidas.keySet());
                yield ids.isEmpty() ? insumoRepository.findAll()
                                    : insumoRepository.findAllById(ids);
            }
            default -> throw new BusinessException(
                    "Tipo de informe inválido: '" + tipo + "'. Valores aceptados: "
                    + "GENERAL | ENTRADAS | SALIDAS | STOCK_BAJO | ROTACION | CONSUMO_PROMEDIO | PEDIDOS");
        };
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ROTACIÓN DE INVENTARIO
    // ══════════════════════════════════════════════════════════════════════

    private ReporteDTO.Response generarRotacion(ReporteDTO.FiltroRequest filtro,
                                                 LocalDate inicio, LocalDate fin) {
        long diasPeriodo = ChronoUnit.DAYS.between(inicio, fin) + 1;
        double mesesPeriodo = diasPeriodo / 30.0;

        // Salidas confirmadas en el periodo → totales por insumo
        Map<Long, Integer> salidasMap = salidaRepository.findByFechaBetween(inicio, fin)
                .stream()
                .filter(s -> EstadoMovimiento.CONFIRMADA == s.getEstado())
                .flatMap(s -> s.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleSalida::getCantidad)));

        List<Insumo> todosInsumos = insumoRepository.findAll();

        List<ReporteDTO.RotacionItem> rotacion = todosInsumos.stream()
                .map(i -> {
                    int totalSalidas = salidasMap.getOrDefault(i.getId(), 0);
                    boolean muerto   = totalSalidas == 0 && i.getStock() > 0;

                    // Índice rotación = unidades salidas / meses del periodo
                    BigDecimal indice = mesesPeriodo > 0 && totalSalidas > 0
                            ? BigDecimal.valueOf(totalSalidas)
                                    .divide(BigDecimal.valueOf(mesesPeriodo), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // Días de cobertura = stock / (salidas/día)
                    Integer diasCobertura = null;
                    if (totalSalidas > 0) {
                        double salidasDiarias = (double) totalSalidas / diasPeriodo;
                        diasCobertura = salidasDiarias > 0
                                ? (int) Math.round(i.getStock() / salidasDiarias)
                                : null;
                    }

                    String categoria;
                    if (totalSalidas == 0)                    categoria = "Sin movimiento";
                    else if (indice.compareTo(BD_10) >= 0)   categoria = "Alta rotación";
                    else if (indice.compareTo(BD_3)  >= 0)   categoria = "Media rotación";
                    else                                       categoria = "Baja rotación";

                    return ReporteDTO.RotacionItem.builder()
                            .insumoId(i.getId())
                            .nombreInsumo(i.getNombre())
                            .unidadMedida(i.getUnidadMedida())
                            .tipo(i.getTipo())
                            .stockActual(i.getStock())
                            .totalSalidas(totalSalidas)
                            .indiceRotacion(indice)
                            .diasCobertura(diasCobertura)
                            .categoriaRotacion(categoria)
                            .stockMuerto(muerto)
                            .build();
                })
                // Ordenar: sin movimiento al final, luego por índice desc
                .sorted(Comparator
                        .comparingInt((ReporteDTO.RotacionItem r) -> r.isStockMuerto() ? 1 : 0)
                        .thenComparing(Comparator.comparing(
                                ReporteDTO.RotacionItem::getIndiceRotacion).reversed()))
                .toList();

        long altaRotacion  = rotacion.stream().filter(r -> "Alta rotación".equals(r.getCategoriaRotacion())).count();
        long stockMuerto   = rotacion.stream().filter(ReporteDTO.RotacionItem::isStockMuerto).count();

        log.info("Reporte ROTACION — {} insumos | {} alta rotación | {} muertos | {} → {}",
                rotacion.size(), altaRotacion, stockMuerto, inicio, fin);

        return ReporteDTO.Response.builder()
                .rotacion(rotacion)
                .resumen(ReporteDTO.ResumenResponse.builder()
                        .tipoInforme("ROTACION")
                        .fechaInicio(filtro.getFechaInicio())
                        .fechaFin(filtro.getFechaFin())
                        .generadoEn(LocalDateTime.now().format(DT_FMT))
                        .totalInsumos(rotacion.size())
                        .insumosAltaRotacion((int) altaRotacion)
                        .insumosStockMuerto((int) stockMuerto)
                        .build())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSUMO PROMEDIO
    // ══════════════════════════════════════════════════════════════════════

    private ReporteDTO.Response generarConsumoPromedio(ReporteDTO.FiltroRequest filtro,
                                                        LocalDate inicio, LocalDate fin) {
        long diasPeriodo  = ChronoUnit.DAYS.between(inicio, fin) + 1;
        LocalDate mitad   = inicio.plusDays(diasPeriodo / 2);

        // Salidas confirmadas en el periodo completo
        List<Salida> salidasPeriodo = salidaRepository.findByFechaBetween(inicio, fin)
                .stream()
                .filter(s -> EstadoMovimiento.CONFIRMADA == s.getEstado())
                .toList();

        // Salidas primera mitad
        final LocalDate mitadFinal = mitad;
        Map<Long, Integer> primeraMap = salidasPeriodo.stream()
                .filter(s -> !s.getFecha().isAfter(mitadFinal))
                .flatMap(s -> s.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleSalida::getCantidad)));

        // Salidas segunda mitad
        Map<Long, Integer> segundaMap = salidasPeriodo.stream()
                .filter(s -> s.getFecha().isAfter(mitadFinal))
                .flatMap(s -> s.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleSalida::getCantidad)));

        // Total por insumo (todo el periodo)
        Map<Long, Integer> totalMap = salidasPeriodo.stream()
                .flatMap(s -> s.getDetalles().stream())
                .collect(Collectors.groupingBy(
                        d -> d.getInsumo().getId(),
                        Collectors.summingInt(DetalleSalida::getCantidad)));

        // Solo insumos con consumo > 0
        Set<Long> idsConConsumo = totalMap.keySet();
        if (idsConConsumo.isEmpty()) {
            return ReporteDTO.Response.builder()
                    .consumo(Collections.emptyList())
                    .resumen(ReporteDTO.ResumenResponse.builder()
                            .tipoInforme("CONSUMO_PROMEDIO")
                            .fechaInicio(filtro.getFechaInicio())
                            .fechaFin(filtro.getFechaFin())
                            .generadoEn(LocalDateTime.now().format(DT_FMT))
                            .totalInsumos(0)
                            .insumosTendenciaCreciente(0)
                            .insumosTendenciaDecreciente(0)
                            .build())
                    .build();
        }

        List<Insumo> insumosConConsumo = insumoRepository.findAllById(idsConConsumo);

        List<ReporteDTO.ConsumoItem> consumo = insumosConConsumo.stream()
                .map(i -> {
                    int total        = totalMap.getOrDefault(i.getId(), 0);
                    int primera      = primeraMap.getOrDefault(i.getId(), 0);
                    int segunda      = segundaMap.getOrDefault(i.getId(), 0);

                    BigDecimal diario   = BigDecimal.valueOf(total)
                            .divide(BigDecimal.valueOf(diasPeriodo), 2, RoundingMode.HALF_UP);
                    BigDecimal semanal  = diario.multiply(BD_7).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal mensual  = diario.multiply(BD_30).setScale(2, RoundingMode.HALF_UP);

                    // Tendencia: segunda mitad vs primera mitad (umbral 10%)
                    String tendencia = calcularTendencia(primera, segunda);

                    // Días de cobertura
                    Integer diasCobertura = diario.compareTo(BigDecimal.ZERO) > 0
                            ? BigDecimal.valueOf(i.getStock())
                                    .divide(diario, 0, RoundingMode.FLOOR).intValue()
                            : null;

                    return ReporteDTO.ConsumoItem.builder()
                            .insumoId(i.getId())
                            .nombreInsumo(i.getNombre())
                            .unidadMedida(i.getUnidadMedida())
                            .tipo(i.getTipo())
                            .stockActual(i.getStock())
                            .totalConsumo(total)
                            .consumoDiario(diario)
                            .consumoSemanal(semanal)
                            .consumoMensual(mensual)
                            .tendencia(tendencia)
                            .diasCoberturaEstimados(diasCobertura)
                            .build();
                })
                .sorted(Comparator.comparing(ReporteDTO.ConsumoItem::getConsumoMensual).reversed())
                .toList();

        long creciente  = consumo.stream().filter(c -> "Creciente".equals(c.getTendencia())).count();
        long decreciente = consumo.stream().filter(c -> "Decreciente".equals(c.getTendencia())).count();

        log.info("Reporte CONSUMO — {} insumos | {} creciente | {} decreciente | {} → {}",
                consumo.size(), creciente, decreciente, inicio, fin);

        return ReporteDTO.Response.builder()
                .consumo(consumo)
                .resumen(ReporteDTO.ResumenResponse.builder()
                        .tipoInforme("CONSUMO_PROMEDIO")
                        .fechaInicio(filtro.getFechaInicio())
                        .fechaFin(filtro.getFechaFin())
                        .generadoEn(LocalDateTime.now().format(DT_FMT))
                        .totalInsumos(consumo.size())
                        .insumosTendenciaCreciente((int) creciente)
                        .insumosTendenciaDecreciente((int) decreciente)
                        .build())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PEDIDOS — con semáforo
    // ══════════════════════════════════════════════════════════════════════

    private ReporteDTO.Response generarPedidos(ReporteDTO.FiltroRequest filtro,
                                                LocalDate inicio, LocalDate fin) {
        LocalDate hoy = LocalDate.now();

        // Cargar pedidos creados en el periodo (usando paginación sin límite real)
        List<Pedido> pedidos = pedidoRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10000))
                .getContent()
                .stream()
                .filter(p -> {
                    LocalDate creado = p.getCreatedAt() != null
                            ? p.getCreatedAt().toLocalDate() : null;
                    return creado != null && !creado.isBefore(inicio) && !creado.isAfter(fin);
                })
                // Filtro opcional por colegio
                .filter(p -> filtro.getColegioId() == null
                        || (p.getColegio() != null
                        && p.getColegio().getId().equals(filtro.getColegioId())))
                // Filtro opcional por estado
                .filter(p -> filtro.getEstadoPedido() == null
                        || p.getEstado().name().equalsIgnoreCase(filtro.getEstadoPedido()))
                .toList();

        List<ReporteDTO.PedidoItem> items = pedidos.stream()
                .map(p -> {
                    String semaforo;
                    String semaforoDesc;
                    Integer diasRestantes = null;

                    if (p.esEntregado()) {
                        semaforo     = "ENTREGADO";
                        semaforoDesc = "Entregado";
                    } else if (p.esCancelado()) {
                        semaforo     = "CANCELADO";
                        semaforoDesc = "Cancelado";
                    } else if (p.getFechaEstimadaEntrega() == null) {
                        semaforo     = "SIN_FECHA";
                        semaforoDesc = "Sin fecha de entrega";
                    } else {
                        diasRestantes = (int) ChronoUnit.DAYS.between(hoy, p.getFechaEstimadaEntrega());
                        if (diasRestantes < 0) {
                            semaforo     = "ROJO";
                            semaforoDesc = "Retrasado " + Math.abs(diasRestantes) + " día(s)";
                        } else if (diasRestantes <= 7) {
                            semaforo     = "AMARILLO";
                            semaforoDesc = "Próximo — " + diasRestantes + " día(s) restantes";
                        } else {
                            semaforo     = "VERDE";
                            semaforoDesc = "A tiempo — " + diasRestantes + " día(s) restantes";
                        }
                    }

                    Integer pct = p.getFactorCumplimiento() != null
                            ? p.getFactorCumplimiento()
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(0, RoundingMode.HALF_UP)
                                    .intValue()
                            : null;

                    return ReporteDTO.PedidoItem.builder()
                            .pedidoId(p.getId())
                            .numeroPedido(p.getNumeroPedido())
                            .colegio(p.getColegio() != null ? p.getColegio().getNombre() : null)
                            .estado(p.getEstado().name())
                            .estadoDescripcion(describirEstado(p.getEstado()))
                            .fechaPedido(p.getCreatedAt() != null
                                    ? p.getCreatedAt().format(DT_FMT) : null)
                            .fechaEstimadaEntrega(p.getFechaEstimadaEntrega() != null
                                    ? p.getFechaEstimadaEntrega().toString() : null)
                            .diasRestantes(diasRestantes)
                            .semaforo(semaforo)
                            .semaforoDescripcion(semaforoDesc)
                            .totalPrendas(p.getDetalles() != null ? p.getDetalles().size() : 0)
                            .porcentajeCumplimiento(pct)
                            .creadoPor(p.getCreadoPor())
                            .build();
                })
                // Orden: ROJO primero, luego AMARILLO, luego VERDE, luego los demás
                .sorted(Comparator.comparingInt(pi -> semaforoOrden(pi.getSemaforo())))
                .toList();

        long verdes     = items.stream().filter(i -> "VERDE".equals(i.getSemaforo())).count();
        long amarillos  = items.stream().filter(i -> "AMARILLO".equals(i.getSemaforo())).count();
        long rojos      = items.stream().filter(i -> "ROJO".equals(i.getSemaforo())).count();
        long entregados = items.stream().filter(i -> "ENTREGADO".equals(i.getSemaforo())).count();
        long cancelados = items.stream().filter(i -> "CANCELADO".equals(i.getSemaforo())).count();

        log.info("Reporte PEDIDOS — {} total | 🔴{} | 🟡{} | 🟢{} | {} → {}",
                items.size(), rojos, amarillos, verdes, inicio, fin);

        return ReporteDTO.Response.builder()
                .pedidos(items)
                .resumen(ReporteDTO.ResumenResponse.builder()
                        .tipoInforme("PEDIDOS")
                        .fechaInicio(filtro.getFechaInicio())
                        .fechaFin(filtro.getFechaFin())
                        .generadoEn(LocalDateTime.now().format(DT_FMT))
                        .totalPedidos(items.size())
                        .pedidosVerdes((int) verdes)
                        .pedidosAmarillos((int) amarillos)
                        .pedidosRojos((int) rojos)
                        .pedidosEntregados((int) entregados)
                        .pedidosCancelados((int) cancelados)
                        .build())
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════

    private static final BigDecimal BD_3  = BigDecimal.valueOf(3);
    private static final BigDecimal BD_7  = BigDecimal.valueOf(7);
    private static final BigDecimal BD_10 = BigDecimal.valueOf(10);
    private static final BigDecimal BD_30 = BigDecimal.valueOf(30);

    private String calcularTendencia(int primera, int segunda) {
        if (primera == 0 && segunda == 0) return "Sin datos";
        if (primera == 0)                 return "Creciente";
        if (segunda == 0)                 return "Decreciente";

        // Umbral 10% de cambio
        double cambio = ((double)(segunda - primera) / primera) * 100;
        if (cambio >  10) return "Creciente";
        if (cambio < -10) return "Decreciente";
        return "Estable";
    }

    private int semaforoOrden(String semaforo) {
        return switch (semaforo) {
            case "ROJO"      -> 0;
            case "AMARILLO"  -> 1;
            case "SIN_FECHA" -> 2;
            case "VERDE"     -> 3;
            case "ENTREGADO" -> 4;
            case "CANCELADO" -> 5;
            default          -> 6;
        };
    }

    private String describirEstado(EstadoPedido estado) {
        if (estado == null) return null;
        return switch (estado) {
            case BORRADOR           -> "En edición";
            case CALCULADO          -> "Stock verificado";
            case CONFIRMADO         -> "Confirmado";
            case EN_PRODUCCION      -> "En producción";
            case LISTO_PARA_ENTREGA -> "Listo para entrega";
            case ENTREGADO          -> "Entregado";
            case CANCELADO          -> "Cancelado";
        };
    }

    private LocalDate parsearFecha(String fecha) {
        try {
            return LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                    "Formato de fecha inválido: '" + fecha + "'. Use yyyy-MM-dd");
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
