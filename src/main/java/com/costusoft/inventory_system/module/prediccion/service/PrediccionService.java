package com.costusoft.inventory_system.module.prediccion.service;

import com.costusoft.inventory_system.module.prediccion.client.PrediccionClient;
import com.costusoft.inventory_system.module.prediccion.dto.PrediccionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrediccionService {

    private final PrediccionClient prediccionClient;

    public PrediccionDTO.Response predecir(Long insumoId) {
        log.info("Solicitando prediccion para insumo id: {}", insumoId);
        return prediccionClient.predecirInsumo(insumoId);
    }

    public PrediccionDTO.MasivaResponse predecirTodos() {
        log.info("Solicitando prediccion masiva");
        return prediccionClient.predecirTodos();
    }

    public PrediccionDTO.EntrenamientoResponse entrenar() {
        log.info("Disparando reentrenamiento del modelo");
        return prediccionClient.entrenar();
    }

    public boolean servicioDisponible() {
        return prediccionClient.isServiceUp();
    }
}