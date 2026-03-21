package com.costusoft.inventory_system.module.calculadora.service;

import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.entity.Uniforme;
import com.costusoft.inventory_system.entity.UniformeInsumo;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.UniformeRepository;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de la calculadora de disponibilidad.
 *
 * Verifica si hay stock suficiente para fabricar N unidades
 * de un uniforme dado, insumo por insumo, con detalle completo.
 *
 * NO modifica stock — es una operacion de solo lectura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalculadoraService {

    private final UniformeRepository uniformeRepository;
    private final InsumoRepository insumoRepository;

    public CalculadoraDTO.Response verificarDisponibilidad(CalculadoraDTO.Request request) {
        Uniforme uniforme = uniformeRepository.findByIdWithInsumos(request.getUniformeId())
                .orElseThrow(() -> new ResourceNotFoundException("Uniforme", request.getUniformeId()));

        int cantidad = request.getCantidad();
        List<CalculadoraDTO.DetalleInsumo> detalles = new ArrayList<>();
        boolean todoDisponible = true;

        for (UniformeInsumo ui : uniforme.getInsumosRequeridos()) {
            Insumo insumo = insumoRepository.findById(ui.getInsumo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Insumo", ui.getInsumo().getId()));

            // ── Todo en BigDecimal para coincidir con el tipo de cantidadBase ──

            // Error 1 corregido: ui.getCantidadBase() es BigDecimal,
            // multiplicamos por BigDecimal.valueOf(cantidad) en vez de int directo
            BigDecimal necesario = ui.getCantidadBase().multiply(BigDecimal.valueOf(cantidad));

            // Error 2 corregido: stockActual como BigDecimal para comparar con necesario
            BigDecimal stockActual = BigDecimal.valueOf(insumo.getStock());
            BigDecimal restante = stockActual.subtract(necesario);

            // compareTo retorna negativo si restante < ZERO
            boolean suficiente = restante.compareTo(BigDecimal.ZERO) >= 0;

            if (!suficiente)
                todoDisponible = false;

            // Error 1 (segundo): stockActual es BigDecimal, comparar con ZERO correcto
            String estado;
            if (stockActual.compareTo(BigDecimal.ZERO) == 0)
                estado = "Sin stock";
            else if (!suficiente)
                estado = "Insuficiente";
            else
                estado = "Disponible";

            // Error 3 corregido: stockRestante usa max de BigDecimal, no
            // Math.max(BigDecimal, int)
            BigDecimal stockRestante = restante.max(BigDecimal.ZERO);

            detalles.add(CalculadoraDTO.DetalleInsumo.builder()
                    .nombreInsumo(insumo.getNombre())
                    .unidadMedida(ui.getUnidadMedida())
                    .cantidadNecesaria(necesario) // BigDecimal ✅
                    .stockActual(stockActual) // BigDecimal ✅
                    .stockRestante(stockRestante) // BigDecimal.max() ✅
                    .suficiente(suficiente)
                    .estado(estado)
                    .build());
        }

        log.debug("Calculadora — uniforme: '{}' | cantidad: {} | disponible: {}",
                uniforme.getPrenda(), cantidad, todoDisponible);

        return CalculadoraDTO.Response.builder()
                .uniformeId(uniforme.getId())
                .nombrePrenda(uniforme.getPrenda())
                .cantidadSolicitada(cantidad)
                .disponible(todoDisponible)
                .detalles(detalles)
                .build();
    }
}