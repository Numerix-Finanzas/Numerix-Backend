package com.upc.numerix.finanzas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/*
 Fórmulas del marco conceptual (apartado 6.3) con las convenciones del Anexo A:
 - Año comercial de 360 días y mes comercial de 30 días.
 - Tasas efectivas (TED, TEM) y factores redondeados a exactamente 7 decimales.
 - Importes monetarios redondeados a 2 decimales.
 Clase sin estado: todas las operaciones son estáticas y se prueban de forma aislada.
 */
public final class CalculadoraFinanciera {

    public static final int BASE_DIAS = 360;
    public static final int DIAS_MES_COMERCIAL = 30;

    private CalculadoraFinanciera() {
    }

    // ---------- Redondeos ----------

    public static BigDecimal redondear2(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal redondear7(double valor) {
        return BigDecimal.valueOf(valor).setScale(7, RoundingMode.HALF_UP);
    }

    // ---------- Fechas ----------

    // Días entre dos fechas con meses comerciales de 30 días (30/360); el día 31 cuenta como 30
    public static int diasEntre(LocalDate fechaInicial, LocalDate fechaFinal) {
        int diaInicial = Math.min(fechaInicial.getDayOfMonth(), DIAS_MES_COMERCIAL);
        int diaFinal = Math.min(fechaFinal.getDayOfMonth(), DIAS_MES_COMERCIAL);
        return BASE_DIAS * (fechaFinal.getYear() - fechaInicial.getYear())
                + DIAS_MES_COMERCIAL * (fechaFinal.getMonthValue() - fechaInicial.getMonthValue())
                + (diaFinal - diaInicial);
    }

    // Fecha con el día pactado en el mes indicado (en febrero el día 29/30 pasa al último día)
    public static LocalDate conDia(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }

    /*
     Fecha de pago del ciclo al que pertenece una fecha: si la operación se realizó hasta el día
     de corte (23:59:59) se paga en la fecha de pago de ese corte; si fue posterior, en la del corte
     del mes siguiente. Si el día de pago es menor o igual al día de corte, el pago cae en el mes
     siguiente al corte.
     */
    public static LocalDate fechaPagoDelCiclo(LocalDate fecha, int diaCorte, int diaPagoPactado) {
        YearMonth mes = YearMonth.from(fecha);
        if (fecha.getDayOfMonth() > diaCorte) {
            mes = mes.plusMonths(1);
        }
        if (diaPagoPactado <= diaCorte) {
            mes = mes.plusMonths(1);
        }
        return conDia(mes, diaPagoPactado);
    }

    // ---------- Tasas ----------

    public static int diasDelPeriodo(String periodoTasa) {
        return switch (periodoTasa) {
            case "Diario" -> 1;
            case "Mensual" -> 30;
            case "Trimestral" -> 90;
            case "Semestral" -> 180;
            case "Anual" -> 360;
            default -> throw new IllegalArgumentException("Periodo de tasa no válido: " + periodoTasa);
        };
    }

    // Tasa efectiva del periodo pactado; si es nominal: TEP = (1 + TN/m)^m - 1, m = días del periodo / días de capitalización
    public static double tasaEfectivaPeriodo(String tipoTasa, BigDecimal porcentaje, String periodoTasa,
                                             Integer periodoCapitalizacion) {
        double tasa = porcentaje.doubleValue() / 100;
        if ("Nominal".equals(tipoTasa)) {
            double m = (double) diasDelPeriodo(periodoTasa) / periodoCapitalizacion;
            return Math.pow(1 + tasa / m, m) - 1;
        }
        return tasa;
    }

    // Tasa efectiva equivalente para un periodo de "dias" días: TEP2 = (1 + TEP1)^(n2/n1) - 1 (7 decimales)
    public static BigDecimal tasaEquivalente(String tipoTasa, BigDecimal porcentaje, String periodoTasa,
                                             Integer periodoCapitalizacion, int dias) {
        double tep = tasaEfectivaPeriodo(tipoTasa, porcentaje, periodoTasa, periodoCapitalizacion);
        return redondear7(Math.pow(1 + tep, (double) dias / diasDelPeriodo(periodoTasa)) - 1);
    }

    public static BigDecimal tasaEfectivaDiaria(String tipoTasa, BigDecimal porcentaje, String periodoTasa,
                                                Integer periodoCapitalizacion) {
        return tasaEquivalente(tipoTasa, porcentaje, periodoTasa, periodoCapitalizacion, 1);
    }

    public static BigDecimal tasaEfectivaMensual(String tipoTasa, BigDecimal porcentaje, String periodoTasa,
                                                 Integer periodoCapitalizacion) {
        return tasaEquivalente(tipoTasa, porcentaje, periodoTasa, periodoCapitalizacion, DIAS_MES_COMERCIAL);
    }

    // ---------- Interés compuesto ----------

    // Factor (1 + tasa)^periodos redondeado a 7 decimales
    public static BigDecimal factor(BigDecimal tasa, double periodos) {
        return redondear7(Math.pow(1 + tasa.doubleValue(), periodos));
    }

    // I = C * ((1 + tasa)^periodos - 1): interés compensatorio de un solo pago o interés moratorio
    public static BigDecimal interes(BigDecimal capital, BigDecimal tasa, double periodos) {
        if (periodos <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return redondear2(capital.multiply(factor(tasa, periodos).subtract(BigDecimal.ONE)));
    }

    // CA = C * (1 + TED)^dg: capital actualizado tras los días de gracia total
    public static BigDecimal capitalActualizado(BigDecimal capital, BigDecimal ted, int diasGracia) {
        return redondear2(capital.multiply(factor(ted, Math.max(diasGracia, 0))));
    }

    // C = S / (1 + tasa)^periodos: valor presente de un importe futuro (prepago)
    public static BigDecimal valorPresente(BigDecimal importeFuturo, BigDecimal tasa, double periodos) {
        if (periodos <= 0) {
            return redondear2(importeFuturo);
        }
        return importeFuturo.divide(factor(tasa, periodos), 2, RoundingMode.HALF_UP);
    }

    // ---------- Método francés vencido ordinario ----------

    // R = C * TEP * (1 + TEP)^n / ((1 + TEP)^n - 1)
    public static BigDecimal cuotaFrances(BigDecimal capital, BigDecimal tep, int n) {
        double i = tep.doubleValue();
        double potencia = Math.pow(1 + i, n);
        return redondear2(BigDecimal.valueOf(capital.doubleValue() * i * potencia / (potencia - 1)));
    }

    public record FilaCronograma(int numeroCuota, LocalDate fechaVencimiento, BigDecimal saldoInicial,
                                 BigDecimal interes, BigDecimal amortizacion, BigDecimal cuota,
                                 BigDecimal saldoFinal) {
    }

    /*
     Cronograma con cuota constante. Ik = TEP * SIk, Ak = R - Ik, SFk = SIk - Ak.
     La última cuota se ajusta para que el saldo final sea 0.00: An = SIn, Rn = SIn + In.
     */
    public static List<FilaCronograma> cronogramaFrances(BigDecimal capital, BigDecimal tem, int n,
                                                         LocalDate primeraFecha, int diaPagoPactado) {
        List<FilaCronograma> filas = new ArrayList<>();
        BigDecimal cuota = cuotaFrances(capital, tem, n);
        BigDecimal saldoInicial = redondear2(capital);
        YearMonth mes = YearMonth.from(primeraFecha);
        for (int k = 1; k <= n; k++) {
            BigDecimal interes = redondear2(saldoInicial.multiply(tem));
            BigDecimal amortizacion;
            BigDecimal cuotaPeriodo;
            if (k < n) {
                amortizacion = cuota.subtract(interes);
                cuotaPeriodo = cuota;
            } else {
                amortizacion = saldoInicial;
                cuotaPeriodo = saldoInicial.add(interes);
            }
            BigDecimal saldoFinal = saldoInicial.subtract(amortizacion);
            filas.add(new FilaCronograma(k, conDia(mes.plusMonths(k - 1), diaPagoPactado), saldoInicial,
                    interes, amortizacion, cuotaPeriodo, saldoFinal));
            saldoInicial = saldoFinal;
        }
        return filas;
    }
}
