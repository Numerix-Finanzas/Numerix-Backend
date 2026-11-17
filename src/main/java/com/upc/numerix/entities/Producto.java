package com.upc.numerix.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "producto")
public class Producto {
    @Id
    @Column(name = "id_producto", length = 20)
    private String idProducto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_negocio")
    private Negocio negocio;

    @Column(name = "descripcion_producto", nullable = false, length = 150)
    private String descripcionProducto;

    @Column(name = "marca_producto", length = 50)
    private String marcaProducto;

    @Column(name = "proveedor_producto", length = 100)
    private String proveedorProducto;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "imagen_producto", length = 255)
    private String imagenProducto;

    @Column(name = "precio_contado", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioContado;

    @Column(name = "precio_credito", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioCredito;

    @Column(name = "modalidad_venta_producto", nullable = false, length = 10)
    private String modalidadVentaProducto;

    @Column(name = "estado_producto", nullable = false, length = 10)
    private String estadoProducto;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
