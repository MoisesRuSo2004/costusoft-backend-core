package com.costusoft.inventory_system.module.dashboard.service;

import com.costusoft.inventory_system.module.dashboard.dto.DashboardDTO;

public interface DashboardService {

    /**
     * Genera el resumen completo del dashboard.
     * Todas las consultas son readOnly para maxima performance.
     */
    DashboardDTO generarResumen();
}