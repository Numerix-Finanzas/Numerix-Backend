package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resultado de simular o registrar una compra a crédito (resumen y confirmación de los mockups)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraResultadoDTO {
    private String estadoOperacion;
    private String mensaje;
    private String idCompra;
    private String idObligacion;
    private String idCliente;
    private String nombreCliente;
    private String descripcionProducto;
    private String moneda;
    private LocalDate fechaCompra;
    private Integer cantidadProducto;
    private BigDecimal precioCredito;
    private BigDecimal montoCompra;
    private String tipoPago;
    private Integer numeroCuotas;
    private BigDecimal limiteCredito;
    private BigDecimal deudaActual;
    private BigDecimal creditoDisponible;
    private BigDecimal creditoDisponibleDespues;
    private BigDecimal tedCompensatoria;
    private BigDecimal temCompensatoria;
    private LocalDate fechaPagoPactada;
    private LocalDate proximaFechaPago;
    private Integer diasOperacion;
    private BigDecimal interesCompensatorioDeuda;
    private BigDecimal montoTotalPagar;
    private Integer diasGracia;
    private BigDecimal factorCapitalizacion;
    private BigDecimal capitalActualizado;
    private BigDecimal montoCuota;
}
