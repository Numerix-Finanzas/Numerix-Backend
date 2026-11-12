package com.upc.products.entities;

import com.upc.numerix.entities.Obligacion;
import com.upc.numerix.entities.Producto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compra")
public class Compra {
    @Id
    @Column(name = "id_compra", length = 20)
    private String idCompra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto")
    private Producto producto;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "cantidad_producto", nullable = false)
    private Integer cantidadProducto;

    @Column(name = "tipo_pago", nullable = false, length = 15)
    private String tipoPago;

    @Column(name = "numero_cuotas", nullable = false)
    private Integer numeroCuotas;

    @Column(name = "precio_credito_aplicado", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioCreditoAplicado;

    @Column(name = "monto_compra", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoCompra;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToOne(mappedBy = "compra")
    private Obligacion obligacion;
}
