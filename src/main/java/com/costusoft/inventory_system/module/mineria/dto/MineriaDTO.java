package com.costusoft.inventory_system.module.mineria.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
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

    /** Prediccion dinamica: que insumos, cuanto tiempo y con que escenario. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class PronosticoRequest {

        @NotEmpty(message = "Elige al menos un insumo")
        @Size(max = 5, message = "Puedes comparar hasta 5 insumos a la vez")
        private List<@NotNull Long> insumoIds;

        @Min(value = 7, message = "El horizonte minimo es de 7 dias")
        @Max(value = 365, message = "El horizonte maximo es de 365 dias")
        private Integer horizonteDias = 90;

        @Pattern(regexp = "dia|semana|mes", message = "La agrupacion debe ser dia, semana o mes")
        private String agrupacion = "semana";

        @Min(value = 0, message = "El plazo del proveedor no puede ser negativo")
        @Max(value = 90, message = "El plazo del proveedor maximo es de 90 dias")
        private Integer plazoProveedorDias = 7;

        @Valid
        private Escenario escenario;
    }

    /** "¿Y si?": un pedido hipotetico cuyas prendas consumen insumos. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Escenario {

        @NotEmpty(message = "El escenario necesita al menos una prenda")
        @Size(max = 20, message = "El escenario admite hasta 20 prendas")
        private List<@Valid PrendaEscenario> prendas;

        /** Fecha en que se consumiria; si no se indica, al dia siguiente de la fecha de corte. */
        private LocalDate fecha;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PrendaEscenario {

        @NotNull(message = "Indica la prenda (uniformeId)")
        private Long uniformeId;

        @Size(max = 10)
        private String talla;

        @NotNull(message = "Indica la cantidad de prendas")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100_000, message = "La cantidad maxima es 100.000")
        private Integer cantidad;
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
