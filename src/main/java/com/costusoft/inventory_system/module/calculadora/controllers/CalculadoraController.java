package com.costusoft.inventory_system.module.calculadora.controllers;

import com.costusoft.inventory_system.module.calculadora.dto.CalculadoraDTO;
import com.costusoft.inventory_system.module.calculadora.service.CalculadoraService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del modulo Calculadora.
 *
 * Endpoints:
 * POST /api/calculadora/verificar
 * — verifica si hay stock para fabricar N unidades de un uniforme
 *
 * Solo lectura — no modifica ningún dato.
 */
@RestController
@RequestMapping("/api/calculadora")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Calculadora", description = "Verificacion de disponibilidad de insumos por uniforme")
public class CalculadoraController {

    private final CalculadoraService calculadoraService;

    @Operation(summary = "Verificar disponibilidad de insumos", description = "Calcula si hay stock suficiente para fabricar N unidades de un uniforme. "
            +
            "Retorna el detalle insumo por insumo con estado y stock restante.")
    @PostMapping("/verificar")
    public ResponseEntity<ApiResponse<CalculadoraDTO.Response>> verificar(
            @Valid @RequestBody CalculadoraDTO.Request request) {

        CalculadoraDTO.Response resultado = calculadoraService.verificarDisponibilidad(request);

        String mensaje = resultado.isDisponible()
                ? "Stock suficiente para fabricar " + request.getCantidad() + " unidad(es)"
                : "Stock insuficiente para completar el pedido";

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }
}
