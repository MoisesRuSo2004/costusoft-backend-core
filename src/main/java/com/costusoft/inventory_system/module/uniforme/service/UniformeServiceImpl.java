package com.costusoft.inventory_system.module.uniforme.service;

import com.costusoft.inventory_system.entity.Colegio;
import com.costusoft.inventory_system.entity.Insumo;
import com.costusoft.inventory_system.entity.Uniforme;
import com.costusoft.inventory_system.entity.UniformeInsumo;
import com.costusoft.inventory_system.repo.ColegioRepository;
import com.costusoft.inventory_system.repo.InsumoRepository;
import com.costusoft.inventory_system.repo.UniformeRepository;
import com.costusoft.inventory_system.exception.BusinessException;
import com.costusoft.inventory_system.exception.ResourceNotFoundException;
import com.costusoft.inventory_system.module.uniforme.dto.UniformeDTO;
import com.costusoft.inventory_system.module.uniforme.mapper.UniformeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementacion del servicio de uniformes.
 *
 * Responsabilidades:
 * - Validar que el colegio existe antes de crear el uniforme
 * - Resolver cada insumo requerido desde BD
 * - Mantener la relacion bidireccional Uniforme <-> UniformeInsumo
 * - En actualizacion: limpiar insumos anteriores y reemplazar
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UniformeServiceImpl implements UniformeService {

    private final UniformeRepository uniformeRepository;
    private final ColegioRepository colegioRepository;
    private final InsumoRepository insumoRepository;
    private final UniformeMapper uniformeMapper;

    // ── Crear ────────────────────────────────────────────────────────────

    @Override
    public UniformeDTO.Response crear(UniformeDTO.Request request) {
        Colegio colegio = findColegioOrThrow(request.getColegioId());

        validarPrendaUnicaPorColegio(request.getPrenda(), request.getColegioId(), null);

        Uniforme uniforme = new Uniforme();
        uniforme.setPrenda(request.getPrenda());
        uniforme.setTipo(request.getTipo());
        uniforme.setTalla(request.getTalla());
        uniforme.setGenero(request.getGenero());
        uniforme.setColegio(colegio);

        // Resolver y asociar insumos requeridos si vienen
        if (request.getInsumosRequeridos() != null && !request.getInsumosRequeridos().isEmpty()) {
            List<UniformeInsumo> insumosRequeridos = buildInsumosRequeridos(
                    request.getInsumosRequeridos(), uniforme);
            insumosRequeridos.forEach(uniforme::agregarInsumo);
        }

        Uniforme guardado = uniformeRepository.save(uniforme);
        log.info("Uniforme creado — id: {} | prenda: '{}' | colegio: '{}'",
                guardado.getId(), guardado.getPrenda(), colegio.getNombre());

        return toResponseCompleto(guardado);
    }

    // ── Listar por colegio ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<UniformeDTO.Response> listarPorColegio(Long colegioId) {
        findColegioOrThrow(colegioId);

        return uniformeRepository
                .findByColegioIdWithInsumos(colegioId)
                .stream()
                .map(this::toResponseCompleto)
                .toList();
    }

    // ── Obtener por ID ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UniformeDTO.Response obtenerPorId(Long id) {
        Uniforme uniforme = uniformeRepository.findByIdWithInsumos(id)
                .orElseThrow(() -> new ResourceNotFoundException("Uniforme", id));
        return toResponseCompleto(uniforme);
    }

    // ── Actualizar ───────────────────────────────────────────────────────

    @Override
    public UniformeDTO.Response actualizar(Long id, UniformeDTO.Request request) {
        Uniforme uniforme = uniformeRepository.findByIdWithInsumos(id)
                .orElseThrow(() -> new ResourceNotFoundException("Uniforme", id));

        // Si cambia de colegio, validar que el nuevo colegio existe
        if (!uniforme.getColegio().getId().equals(request.getColegioId())) {
            Colegio nuevoColegio = findColegioOrThrow(request.getColegioId());
            uniforme.setColegio(nuevoColegio);
        }

        validarPrendaUnicaPorColegio(request.getPrenda(), request.getColegioId(), id);

        // Actualizar campos basicos
        uniforme.setPrenda(request.getPrenda());
        uniforme.setTipo(request.getTipo());
        uniforme.setTalla(request.getTalla());
        uniforme.setGenero(request.getGenero());

        // Reemplazar insumos requeridos (limpiar + agregar nuevos)
        uniforme.removerInsumo(null); // trigger para limpiar
        uniforme.getInsumosRequeridos().clear();

        if (request.getInsumosRequeridos() != null && !request.getInsumosRequeridos().isEmpty()) {
            List<UniformeInsumo> nuevosInsumos = buildInsumosRequeridos(
                    request.getInsumosRequeridos(), uniforme);
            nuevosInsumos.forEach(uniforme::agregarInsumo);
        }

        Uniforme actualizado = uniformeRepository.save(uniforme);
        log.info("Uniforme actualizado — id: {}", id);

        return toResponseCompleto(actualizado);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────

    @Override
    public void eliminar(Long id) {
        Uniforme uniforme = uniformeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Uniforme", id));
        uniformeRepository.delete(uniforme);
        log.info("Uniforme eliminado — id: {} | prenda: '{}'", id, uniforme.getPrenda());
    }

    // ── Contar ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long contarUniformes() {
        return uniformeRepository.count();
    }

    // ── Helpers privados ─────────────────────────────────────────────────

    private Colegio findColegioOrThrow(Long colegioId) {
        return colegioRepository.findById(colegioId)
                .orElseThrow(() -> new ResourceNotFoundException("Colegio", colegioId));
    }

    /**
     * Valida que no exista ya una prenda con el mismo nombre
     * dentro del mismo colegio (combinacion unica).
     */
    private void validarPrendaUnicaPorColegio(String prenda, Long colegioId, Long idExcluido) {
        boolean existe = (idExcluido == null)
                ? uniformeRepository.existsByPrendaIgnoreCaseAndColegioId(prenda.trim(), colegioId)
                : uniformeRepository.existsByPrendaIgnoreCaseAndColegioId(prenda.trim(), colegioId)
                        && !uniformeRepository.findById(idExcluido)
                                .map(u -> u.getPrenda().equalsIgnoreCase(prenda.trim()))
                                .orElse(false);

        if (existe && idExcluido == null) {
            throw new BusinessException(
                    "Ya existe la prenda '" + prenda.trim() + "' en este colegio");
        }
    }

    /**
     * Construye la lista de UniformeInsumo resolviendo cada insumo desde BD.
     */
    private List<UniformeInsumo> buildInsumosRequeridos(
            List<UniformeDTO.InsumoRequeridoRequest> requests,
            Uniforme uniforme) {

        List<UniformeInsumo> resultado = new ArrayList<>();

        for (UniformeDTO.InsumoRequeridoRequest ir : requests) {
            Insumo insumo = insumoRepository.findById(ir.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", ir.getInsumoId()));

            UniformeInsumo ui = UniformeInsumo.builder()
                    .uniforme(uniforme)
                    .insumo(insumo)
                    .cantidadBase(ir.getCantidadBase())
                    .unidadMedida(ir.getUnidadMedida())
                    .build();

            resultado.add(ui);
        }

        return resultado;
    }

    /**
     * Construye el Response completo mapeando insumos requeridos.
     */
    private UniformeDTO.Response toResponseCompleto(Uniforme uniforme) {
        List<UniformeDTO.InsumoRequeridoResponse> insumosResponse = uniforme
                .getInsumosRequeridos()
                .stream()
                .map(uniformeMapper::insumoRequeridoToResponse)
                .toList();

        return UniformeDTO.Response.builder()
                .id(uniforme.getId())
                .prenda(uniforme.getPrenda())
                .tipo(uniforme.getTipo())
                .talla(uniforme.getTalla())
                .genero(uniforme.getGenero())
                .colegioId(uniforme.getColegio().getId())
                .colegioNombre(uniforme.getColegio().getNombre())
                .insumosRequeridos(insumosResponse)
                .createdAt(uniforme.getCreatedAt() != null
                        ? uniforme.getCreatedAt().toString()
                        : null)
                .updatedAt(uniforme.getUpdatedAt() != null
                        ? uniforme.getUpdatedAt().toString()
                        : null)
                .build();
    }
}
