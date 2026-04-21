package com.costusoft.inventory_system.entity;

import com.costusoft.inventory_system.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Pedido de fabricación de uniformes para un colegio.
 *
 * El colegio actúa como cliente — no existe una entidad Cliente separada.
 *
 * Flujo de inventario:
 *   1. crear()            → BORRADOR, sin tocar stock
 *   2. calcular()         → CALCULADO, almacena resultados de la calculadora
 *   3. confirmar()        → CONFIRMADO, sin tocar stock
 *   4. iniciarProduccion() → EN_PRODUCCION, genera Salida PENDIENTE
 *   5. marcarListo()      → LISTO_PARA_ENTREGA
 *   6. entregar()         → ENTREGADO, BODEGA confirma Salida → stock descontado
 *   cancelar()            → CANCELADO en cualquier estado no-final,
 *                           rechaza Salida si existía
 */
@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedidos_estado",   columnList = "estado"),
        @Index(name = "idx_pedidos_colegio",  columnList = "colegio_id"),
        @Index(name = "idx_pedidos_numero",   columnList = "numero_pedido")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número legible del pedido. Formato: PED-{año}-{id:05d}.
     * Generado en el servicio tras el primer save(), por eso inicia como null.
     */
    @Column(name = "numero_pedido", unique = true, length = 20)
    private String numeroPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 25)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.BORRADOR;

    @Column(name = "fecha_estimada_entrega")
    private LocalDate fechaEstimadaEntrega;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    @Column(name = "observaciones", length = 500)
    private String observaciones;

    // ── Resultado del último calcular() ─────────────────────────────────

    /**
     * Factor de cumplimiento (0.0000 – 1.0000) del último cálculo.
     * 1.0 = pedido 100% atendible con stock actual.
     */
    @Column(name = "factor_cumplimiento", precision = 7, scale = 4)
    private BigDecimal factorCumplimiento;

    /** true si todos los insumos del pedido son suficientes. */
    @Column(name = "disponible_completo")
    private Boolean disponibleCompleto;

    /** Nombre del insumo cuello de botella. null si disponible_completo = true. */
    @Column(name = "insumo_limitante", length = 100)
    private String insumoLimitante;

    // ── Relaciones ───────────────────────────────────────────────────────

    /**
     * Colegio que realiza el pedido — el colegio ES el cliente en este sistema.
     */
    @NotNull(message = "El colegio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colegio_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pedido_colegio"))
    private Colegio colegio;

    /** Username de quien creó el pedido (snapshot inmutable). */
    @Column(name = "creado_por", nullable = false, length = 50)
    private String creadoPor;

    /**
     * Salida de inventario generada al entrar en EN_PRODUCCION.
     * Null hasta ese momento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salida_id", foreignKey = @ForeignKey(name = "fk_pedido_salida"))
    private Salida salida;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<PedidoHistorial> historial = new ArrayList<>();

    // ── Helpers de relación ──────────────────────────────────────────────

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }

    public void limpiarDetalles() {
        detalles.forEach(d -> d.setPedido(null));
        detalles.clear();
    }

    // ── Helpers de estado ────────────────────────────────────────────────

    public boolean esBorrador()          { return EstadoPedido.BORRADOR           == this.estado; }
    public boolean esCalculado()         { return EstadoPedido.CALCULADO          == this.estado; }
    public boolean esConfirmado()        { return EstadoPedido.CONFIRMADO         == this.estado; }
    public boolean esEnProduccion()      { return EstadoPedido.EN_PRODUCCION      == this.estado; }
    public boolean esListoParaEntrega()  { return EstadoPedido.LISTO_PARA_ENTREGA == this.estado; }
    public boolean esEntregado()         { return EstadoPedido.ENTREGADO          == this.estado; }
    public boolean esCancelado()         { return EstadoPedido.CANCELADO          == this.estado; }

    /** true si el pedido ya no puede cambiar de estado. */
    public boolean esFinal()             { return esEntregado() || esCancelado(); }

    /** Solo BORRADOR puede editarse (detalles, fechas, etc.). */
    public boolean esEditable()          { return esBorrador(); }
}
