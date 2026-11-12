package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteDTO {
    private String idCliente;
    private String nombreCliente;
    private String tipoDocumentoCliente;
    private String numeroDocumentoCliente;
    private String estadoCliente;
    private String moneda;
    private BigDecimal limiteCredito;
    private BigDecimal creditoUtilizado;
    private BigDecimal creditoDisponible;
    private String tipoTasa;
    private BigDecimal porcentajeTasa;
    private String tipoTasaMoratoria;
    private BigDecimal porcentajeTasaMoratoria;
    private String periodoTasa;
    private Integer periodoCapitalizacion;
    private Integer diaCorte;
    private Integer diaPagoPactado;
    private Integer maximoMesesCredito;
    private BigDecimal tedCompensatoria;
    private BigDecimal temCompensatoria;
    private BigDecimal tedMoratoria;
    private LocalDateTime fechaRegistro;
}
