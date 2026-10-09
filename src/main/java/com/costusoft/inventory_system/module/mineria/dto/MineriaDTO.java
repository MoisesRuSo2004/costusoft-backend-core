package com.costusoft.inventory_system.module.mineria.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTOs del modulo de Mineria de datos.
 *
 * La entrada se valida aqui (el backend no confia en el navegador) y se envia
 * al microservicio Python. La respuesta del microservicio se reenvia tal cual:
 * su contrato vive en un solo lugar (mineria/esquemas.py).
 */
public class MineriaDTO {

    private MineriaDTO() {
    }

    /** Deteccion de anomalias: periodo, insumos, tipo de movimiento y sensibilidad. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnomaliasRequest {

        /** Por defecto, los 12 meses previos al ultimo movimiento registrado. */
        private LocalDate desde;
        private LocalDate hasta;

        @Size(max = 50, message = "Puedes filtrar hasta 50 insumos")
        private List<@NotNull Long> insumoIds;

        @Pattern(regexp = "salidas|entradas|ambos", message = "El tipo debe ser salidas, entradas o ambos")
        private String tipo = "salidas";

        @Pattern(regexp = "baja|media|alta", message = "La sensibilidad debe ser baja, media o alta")
        private String sensibilidad = "media";

        @Min(value = 1, message = "El limite minimo es 1")
        @Max(value = 500, message = "El limite maximo es 500")
        private Integer limite = 100;

        /** Validar el detector con anomalias simuladas en memoria (no toca la base de datos). */
        private Boolean validar = true;

        @Min(value = 10, message = "Se simulan al menos 10 anomalias")
        @Max(value = 300, message = "Se simulan como maximo 300 anomalias")
        private Integer anomaliasSimuladas = 60;

        @JsonIgnore
        @AssertTrue(message = "La fecha inicial no puede ser posterior a la final")
        public boolean isPeriodoValido() {
            return desde == null || hasta == null || !desde.isAfter(hasta);
        }
    }
}
