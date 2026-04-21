package com.costusoft.inventory_system.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * DTO estandar para respuestas paginadas.
 *
 * Encapsula los metadatos de paginacion junto con el contenido,
 * de modo que el frontend siempre recibe la misma estructura
 * independientemente del modulo.
 *
 * Uso:
 * Page<Insumo> page = insumoRepository.findAll(pageable);
 * return ApiResponse.ok(PageDTO.from(page, insumoMapper::toDTO));
 */
@Getter
public class PageDTO<T> {

    private final List<T> content;

    /**
     * Número de página actual (0-based).
     * Se serializa como "number" para alinear con Spring Data Page
     * y con el tipo PageData<T> del frontend.
     */
    @JsonProperty("number")
    private final int pageNumber;

    /**
     * Elementos por página.
     * Se serializa como "size" para alinear con Spring Data Page
     * y con el tipo PageData<T> del frontend.
     */
    @JsonProperty("size")
    private final int pageSize;

    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    private PageDTO(
            List<T> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    /**
     * Construye un PageDTO desde un Page de Spring Data,
     * aplicando un mapper para convertir las entidades a DTOs.
     *
     * @param page   pagina de Spring Data con entidades
     * @param mapper funcion que convierte cada entidad a su DTO
     */
    public static <E, D> PageDTO<D> from(Page<E> page, Function<E, D> mapper) {
        List<D> mappedContent = page.getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageDTO<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
