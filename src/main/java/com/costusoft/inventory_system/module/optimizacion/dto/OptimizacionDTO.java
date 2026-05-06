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

        @JsonProperty("incluir_demanda")
        private boolean incluirDemanda = true;

        private Map<String, Double> utilidades;

        @JsonProperty("ejecutado_por")
        private String ejecutadoPor;

        public static Request of(boolean incluirDemanda, String usuario) {
            Request r = new Request();
            r.incluirDemanda = incluirDemanda;
            r.ejecutadoPor = usuario;
            return r;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanProduccion {
        @JsonProperty("pantalon_diario")
        private int pantalonDiario;

        @JsonProperty("camisa_diario")
        private int camisaDiario;

        @JsonProperty("sueter_diario")
        private int sueterDiario;

        @JsonProperty("pantalon_ef")
        private int pantalonEf;

        @JsonProperty("sueter_ef")
        private int sueterEf;
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
        private String estado;

        @JsonProperty("utilidad_total")
        private double utilidadTotal;

        private PlanProduccion plan;
        private Map<String, RecursoInfo> recursos;
        private Map<String, Object> demanda;
        private String mensaje;

        @JsonProperty("grafica_html")
        private String graficaHtml;

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
