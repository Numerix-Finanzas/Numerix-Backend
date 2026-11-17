package com.upc.numerix.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Compra a crédito de un cliente con uno o varios productos (detalle_compra)
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

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "tipo_pago", nullable = false, length = 15)
    private String tipoPago;

    @Column(name = "numero_cuotas", nullable = false)
    private Integer numeroCuotas;

    // Suma de los subtotales del detalle
    @Column(name = "monto_compra", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoCompra;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idDetalleCompra ASC")
    private List<DetalleCompra> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "compra")
    private Obligacion obligacion;

    // Nombres de los productos para listados y reportes ("Arroz Costeño 5 kg, Aceite Primor 1 L")
    public String getDescripcionProductos() {
        return detalles.stream().map(d -> d.getProducto().getDescripcionProducto()).collect(Collectors.joining(", "));
    }

    public int getCantidadTotal() {
        return detalles.stream().mapToInt(DetalleCompra::getCantidadProducto).sum();
    }
}
