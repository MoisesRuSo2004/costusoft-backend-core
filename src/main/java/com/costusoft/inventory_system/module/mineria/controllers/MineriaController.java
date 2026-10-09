package com.costusoft.inventory_system.module.mineria.controllers;

import com.costusoft.inventory_system.module.mineria.dto.MineriaDTO;
import com.costusoft.inventory_system.module.mineria.service.MineriaService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mineria")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Mineria de datos", description = "Pronostico dinamico, anomalias y patrones de consumo (microservicio Python)")
public class MineriaController {

    private final MineriaService mineriaService;

    @Operation(summary = "Pronostico dinamico de insumos",
            description = "El usuario elige de 1 a 5 insumos, el horizonte (7-365 dias), la agrupacion "
                    + "(dia, semana o mes), el plazo del proveedor y opcionalmente un escenario '¿y si?' "
                    + "(prendas de un pedido hipotetico). Cada insumo usa el modelo que mejor resulto "
                    + "en el backtesting (Prophet, promedio de 12 meses o estacional).")
    @PostMapping("/pronostico")
    public ResponseEntity<ApiResponse<JsonNode>> pronostico(@Valid @RequestBody MineriaDTO.PronosticoRequest solicitud) {
        return ResponseEntity.ok(ApiResponse.ok("Pronostico calculado", mineriaService.pronostico(solicitud)));
    }

    @Operation(summary = "Deteccion de anomalias en movimientos",
            description = "Movimientos que se alejan de lo habitual para su insumo (cantidades muy altas o "
                    + "muy bajas, repeticiones, combinaciones raras) con estadistica robusta, Isolation Forest "
                    + "y LOF. Incluye la validacion del detector con anomalias simuladas en memoria.")
    @PostMapping("/anomalias")
    public ResponseEntity<ApiResponse<JsonNode>> anomalias(@Valid @RequestBody MineriaDTO.AnomaliasRequest solicitud) {
        return ResponseEntity.ok(ApiResponse.ok("Anomalias detectadas", mineriaService.anomalias(solicitud)));
    }
}
