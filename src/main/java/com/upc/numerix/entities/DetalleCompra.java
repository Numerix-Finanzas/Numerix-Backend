package com.upc.numerix.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Línea de una compra: un producto, su cantidad y el precio crédito vigente al comprar
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detalle_compra", uniqueConstraints = @UniqueConstraint(columnNames = {"id_compra", "id_producto"}))
public class DetalleCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_compra")
    private Long idDetalleCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(name = "cantidad_producto", nullable = false)
    private Integer cantidadProducto;

    @Column(name = "precio_credito_aplicado", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioCreditoAplicado;

    // cantidadProducto × precioCreditoAplicado
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;
}
