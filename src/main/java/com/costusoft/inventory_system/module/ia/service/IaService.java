package com.costusoft.inventory_system.module.ia.service;

import com.costusoft.inventory_system.module.ia.dto.IaDTO;

import java.util.List;

/**
 * Contrato del servicio de Inteligencia Artificial.
 *
 * Define las operaciones disponibles para el módulo IA.
 * La implementación (IaServiceImpl) orquesta: BD → prompt → Groq → respuesta.
 */
public interface IaService {

    /**
     * Ejecuta una consulta de análisis contra el inventario usando IA.
     * Obtiene los datos relevantes de la BD, construye el prompt,
     * llama a Groq y retorna la respuesta en lenguaje natural.
     *
     * @param request Tipo de análisis solicitado
     * @return Respuesta en español + metadatos de la consulta
     */
    IaDTO.ConsultaResponse consultar(IaDTO.ConsultaRequest request);

    /**
     * Verifica si el servicio de IA está disponible y configurado.
     *
     * @return true si la API key está configurada y el cliente inicializado
     */
    boolean isServiceUp();

    /**
     * Retorna la lista de tipos de consulta disponibles.
     *
     * @return Lista de nombres de TipoConsulta
     */
    List<String> getTiposDisponibles();

    /**
     * Chat libre en lenguaje natural sobre el inventario.
     * El backend inyecta un snapshot completo del sistema como contexto.
     *
     * @param request Pregunta del usuario en texto libre
     * @return Respuesta conversacional de Groq
     */
    IaDTO.ChatResponse chat(IaDTO.ChatRequest request);

    /**
     * Chat que permite pasar un contexto estructurado (JSON) específico — ej. para un colegio.
     * El backend NO reinyecta el snapshot completo: usa el contexto proporcionado.
     */
    IaDTO.ChatResponse chatWithContext(String contextoJson, IaDTO.ChatRequest request);

    /**
     * Genera el texto de una orden de compra lista para enviar.
     * Si se especifica proveedorId, la carta se dirige a ese proveedor.
     * Si no, se genera un documento general de requisición.
     *
     * @param request Datos opcionales: proveedorId, nombreEmpresa, observaciones
     * @return Texto formal de la orden + metadatos
     */
    IaDTO.OrdenCompraResponse generarOrdenCompra(IaDTO.OrdenCompraRequest request);

    /**
     * Genera el briefing ejecutivo del día.
     * Combina stock, pedidos, movimientos pendientes y alertas en un resumen matutino.
     *
     * @return Resumen ejecutivo del día en lenguaje natural
     */
    IaDTO.ConsultaResponse getBriefing();
}
