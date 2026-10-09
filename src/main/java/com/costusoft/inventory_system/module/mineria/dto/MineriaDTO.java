package com.costusoft.inventory_system.module.mineria.dto;

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
}
