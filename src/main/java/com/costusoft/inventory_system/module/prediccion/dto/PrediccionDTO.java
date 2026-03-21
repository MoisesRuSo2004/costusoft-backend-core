package com.costusoft.inventory_system.module.prediccion.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTOs del modulo Prediccion — alineados con la respuesta del microservicio
 * Python.
 */
public class PrediccionDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProphetResultado {
        @JsonProperty("dias_hasta_stock_minimo")
        private Integer diasHastaStockMinimo;
        @JsonProperty("dias_hasta_cero")
        private Integer diasHastaCero;
        @JsonProperty("consumo_diario_promedio")
        private Double consumoDiarioPromedio;
        @JsonProperty("fecha_alerta_estimada")
        private String fechaAlertaEstimada;
        @JsonProperty("fecha_agotamiento_estimada")
        private String fechaAgotamientoEstimada;
        private Double confianza;
        @JsonProperty("suficiente_historial")
        private Boolean suficienteHistorial;
        private String metodo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class XGBoostResultado {
        @JsonProperty("nivel_riesgo")
        private String nivelRiesgo;
        private Map<String, Double> probabilidades;
        @JsonProperty("modelo_entrenado")
        private Boolean modeloEntrenado;
        private String metodo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Response {
        @JsonProperty("insumo_id")
        private Long insumoId;
        private String nombre;
        @JsonProperty("stock_actual")
        private Integer stockActual;
        @JsonProperty("stock_minimo")
        private Integer stockMinimo;
        @JsonProperty("unidad_medida")
        private String unidadMedida;
        private ProphetResultado prophet;
        private XGBoostResultado xgboost;
        private Map<String, Object> features;
        private Boolean alerta;
        private String mensaje;
        private String recomendacion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MasivaResponse {
        // En MasivaResponse:
        @JsonProperty("en_riesgo")
        private Integer enRiesgo;
        private Integer total;
        private List<Response> predicciones;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EntrenamientoResponse {
        private Boolean exito;
        private String mensaje;
        private Integer registros;
    }
}
