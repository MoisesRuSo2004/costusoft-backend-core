package com.costusoft.inventory_system.module.prediccion.controllers;

import com.costusoft.inventory_system.module.prediccion.dto.PrediccionDTO;
import com.costusoft.inventory_system.module.prediccion.service.PrediccionService;
import com.costusoft.inventory_system.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediccion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@Tag(name = "Prediccion", description = "Prediccion via microservicio Python (Prophet + XGBoost)")
public class PrediccionController {

        private final PrediccionService prediccionService;

        @Operation(summary = "Estado del servicio de prediccion")
        @GetMapping("/status")
        public ResponseEntity<ApiResponse<Boolean>> status() {
                boolean up = prediccionService.servicioDisponible();
                return ResponseEntity.ok(ApiResponse.ok(
                                up ? "Servicio de prediccion disponible"
                                                : "Servicio de prediccion no disponible",
                                up));
        }

        @Operation(summary = "Predecir agotamiento de un insumo")
        @GetMapping("/{insumoId}")
        public ResponseEntity<ApiResponse<PrediccionDTO.Response>> predecir(
                        @PathVariable Long insumoId) {
                PrediccionDTO.Response resultado = prediccionService.predecir(insumoId);
                String mensaje = Boolean.TRUE.equals(resultado.getAlerta())
                                ? "ALERTA: " + resultado.getMensaje()
                                : resultado.getMensaje();
                return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
        }

        @Operation(summary = "Predecir agotamiento de todos los insumos")
        @GetMapping("/todos")
        public ResponseEntity<ApiResponse<PrediccionDTO.MasivaResponse>> predecirTodos() {
                PrediccionDTO.MasivaResponse resultado = prediccionService.predecirTodos();
                String mensaje = (resultado.getEnRiesgo() == null || resultado.getEnRiesgo() == 0)
                                ? "Ningun insumo en riesgo"
                                : resultado.getEnRiesgo() + " insumo(s) en riesgo";
                return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
        }

        @Operation(summary = "Reentrenar modelo ML — solo ADMIN")
        @PostMapping("/entrenar")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<PrediccionDTO.EntrenamientoResponse>> entrenar() {
                return ResponseEntity.ok(
                                ApiResponse.ok("Reentrenamiento completado",
                                                prediccionService.entrenar()));
        }
}
