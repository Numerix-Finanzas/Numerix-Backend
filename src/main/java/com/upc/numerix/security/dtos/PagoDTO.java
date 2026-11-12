package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Pago registrado (confirmación y fila de Movimientos - Pagos)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private String estadoOperacion;
    private String mensaje;
    private String idPago;
    private String idObligacion;
    private String idCompra;
    private Long idCuota;
    private Integer numeroPeriodo;
    private String descripcionProducto;
    private LocalDate fechaPagoReal;
    private Integer diasMora;
    private Integer diasAnticipacion;
    private BigDecimal montoImputadoMora;
    private BigDecimal montoImputadoInteres;
    private BigDecimal montoImputadoCapital;
    private BigDecimal montoPagado;
    private String estadoDeuda;
    private BigDecimal creditoDisponible;
}
