package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDeudaDTO {
    private String idCliente;
    private String numeroDocumentoCliente;
    private String nombreCliente;
    private String moneda;
    private BigDecimal limiteCredito;
    private BigDecimal creditoUtilizado;
    private BigDecimal creditoDisponible;
    private LocalDate fechaConsulta;
    private LocalDate proximaFechaPago;
    private Integer diaCorte;
    private Integer diaPagoPactado;
    private Integer cantidadObligacionesPendientes;
    private List<DeudaDTO> deudas;
    private BigDecimal capitalTotal;
    private BigDecimal interesCompensatorioTotal;
    private BigDecimal interesMoratorioTotal;
    private BigDecimal montoTotalPagar;
}
