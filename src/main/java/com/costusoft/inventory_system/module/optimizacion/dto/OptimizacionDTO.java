package com.costusoft.inventory_system.module.optimizacion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OptimizacionDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {

        @JsonProperty("colegio_id")
        private Long colegioId;

        @JsonProperty("incluir_demanda")
        private boolean incluirDemanda = true;

        private Map<String, Double> utilidades;

        @JsonProperty("ejecutado_por")
        private String ejecutadoPor;

        public static Request of(Long colegioId, boolean incluirDemanda, String usuario) {
            Request r = new Request();
            r.colegioId     = colegioId;
            r.incluirDemanda = incluirDemanda;
            r.ejecutadoPor  = usuario;
            return r;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecursoInfo {
        private double usado;
        private double disponible;
        private double holgura;

        @JsonProperty("utilizacion_pct")
        private double utilizacionPct;

        @JsonProperty("unidad_medida")
        private String unidadMedida = "";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Integer id;

        @JsonProperty("colegio_id")
        private Long colegioId;

        @JsonProperty("nombre_colegio")
        private String nombreColegio;

        private String estado;

        @JsonProperty("utilidad_total")
        private double utilidadTotal;

        /** Plan dinámico: {var_key → cantidad}. Ej: {"pantalon_diario": 5, "camisa_diario": 3} */
        private Map<String, Integer> plan;

        private Map<String, RecursoInfo> recursos;
        private Map<String, Object> demanda;

        /** {ins_key → "Tela Lacoste Blanca"} — nombres reales de insumos desde la BD */
        @JsonProperty("insumo_labels")
        private Map<String, String> insumoLabels;

        /** {var_key → {cantidad, max_producible, insumo_limitante_nombre, motivo}} */
        @JsonProperty("detalle_plan")
        private Map<String, Object> detallePlan;

        private String mensaje;

        @JsonProperty("grafica_html")
        private String graficaHtml;

        @JsonProperty("grafica_region_html")
        private String graficaRegionHtml;

        @JsonProperty("fecha_ejecucion")
        private LocalDateTime fechaEjecucion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistorialResponse {
        private int total;
        private List<Map<String, Object>> items;
    }
}
