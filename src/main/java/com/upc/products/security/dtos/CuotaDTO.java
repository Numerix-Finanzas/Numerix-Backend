package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuotaDTO {
    private Long idCuota;
    private Integer numeroPeriodo;
    private LocalDate fechaVencimientoPeriodo;
    private BigDecimal saldoInicialPeriodo;
    private BigDecimal interesPeriodo;
    private BigDecimal amortizacionPeriodo;
    private BigDecimal cuotaPeriodo;
    private BigDecimal saldoFinalPeriodo;
    private String estadoCuota;
    private LocalDate fechaPagoReal;
}
