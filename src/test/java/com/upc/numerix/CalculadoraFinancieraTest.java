package com.upc.numerix;

import com.upc.numerix.finanzas.CalculadoraFinanciera;
import com.upc.numerix.finanzas.CalculadoraFinanciera.FilaCronograma;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.upc.numerix.finanzas.CalculadoraFinanciera.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Valores esperados tomados de las tablas de datos intermedios de los juegos de prueba del informe
class CalculadoraFinancieraTest {

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void tasasDelJuego1_TasaEfectivaAnual() {
        assertEquals(bd("0.0007291"), tasaEfectivaDiaria("Efectiva", bd("30"), "Anual", null));
        assertEquals(bd("0.0003883"), tasaEfectivaDiaria("Efectiva", bd("15"), "Anual", null));
    }

    @Test
    void tasasDelJuego2_TasaNominalCapitalizableMensual() {
        assertEquals(bd("0.0200000"), tasaEfectivaMensual("Nominal", bd("24"), "Anual", 30));
        assertEquals(bd("0.0006603"), tasaEfectivaDiaria("Nominal", bd("24"), "Anual", 30));
        assertEquals(bd("0.0003317"), tasaEfectivaDiaria("Nominal", bd("12"), "Anual", 30));
    }

    @Test
    void conteoDeDiasConMesesComercialesDe30Dias() {
        assertEquals(20, diasEntre(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 30)));
        assertEquals(33, diasEntre(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 10, 30)));
        assertEquals(6, diasEntre(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 6)));
        assertEquals(15, diasEntre(LocalDate.of(2026, 12, 11), LocalDate.of(2026, 12, 26)));
        // El día 31 se cuenta como día 30
        assertEquals(15, diasEntre(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31)));    }

    @Test
    void fechaDePagoSegunElDiaDeCorte() {
        assertEquals(LocalDate.of(2026, 9, 30), fechaPagoDelCiclo(LocalDate.of(2026, 9, 25), 25, 30));
        assertEquals(LocalDate.of(2026, 10, 30), fechaPagoDelCiclo(LocalDate.of(2026, 9, 27), 25, 30));
        assertEquals(LocalDate.of(2026, 9, 26), fechaPagoDelCiclo(LocalDate.of(2026, 9, 15), 20, 26));
        // Día de pago menor que el de corte: el pago cae el mes siguiente; en febrero se ajusta al último día
        assertEquals(LocalDate.of(2026, 10, 5), fechaPagoDelCiclo(LocalDate.of(2026, 9, 10), 25, 5));
        assertEquals(LocalDate.of(2026, 2, 28), fechaPagoDelCiclo(LocalDate.of(2026, 2, 10), 20, 30));
    }

    @Test
    void interesCompensatorioYMoratorioDelJuego1() {
        BigDecimal ted = bd("0.0007291");
        assertEquals(bd("1.76"), interes(bd("120.00"), ted, 20));
        assertEquals(bd("0.47"), interes(bd("80.00"), ted, 8));
        assertEquals(bd("1.46"), interes(bd("60.00"), ted, 33));
        assertEquals(bd("0.28"), interes(bd("121.76"), bd("0.0003883"), 6));
        assertEquals(bd("0.19"), interes(bd("80.47"), bd("0.0003883"), 6));
    }

    @Test
    void graciaTotalYMetodoFrancesDelJuego2() {
        assertEquals(bd("1.0072873"), factor(bd("0.0006603"), 11));
        BigDecimal capital = capitalActualizado(bd("450.00"), bd("0.0006603"), 11);
        assertEquals(bd("453.28"), capital);
        assertEquals(bd("157.18"), cuotaFrances(capital, bd("0.0200000"), 3));

        List<FilaCronograma> filas = cronogramaFrances(capital, bd("0.0200000"), 3, LocalDate.of(2026, 10, 26), 26);
        assertEquals(bd("9.07"), filas.get(0).interes());
        assertEquals(bd("148.11"), filas.get(0).amortizacion());
        assertEquals(bd("305.17"), filas.get(0).saldoFinal());
        assertEquals(bd("6.10"), filas.get(1).interes());
        assertEquals(bd("154.09"), filas.get(1).saldoFinal());
        // Última cuota ajustada para cerrar el saldo en cero
        assertEquals(bd("3.08"), filas.get(2).interes());
        assertEquals(bd("157.17"), filas.get(2).cuota());
        assertEquals(bd("0.00"), filas.get(2).saldoFinal());
        assertEquals(LocalDate.of(2026, 12, 26), filas.get(2).fechaVencimiento());
    }

    @Test
    void moraYPrepagoDelJuego2y4() {
        assertEquals(bd("0.21"), interes(bd("157.18"), bd("0.0003317"), 4));
        assertEquals(bd("1.0099505"), factor(bd("0.0200000"), 15.0 / CalculadoraFinanciera.DIAS_MES_COMERCIAL));
        assertEquals(bd("155.62"), valorPresente(bd("157.17"), bd("0.0200000"), 15.0 / 30));
    }

    // ---------- Casos adicionales a los juegos de prueba (valores calculados con precisión de 50 dígitos) ----------

    @Test
    void conversionDeTasasConOtrosPeriodos() {
        // Efectiva mensual a diaria
        assertEquals(bd("0.0006603"), tasaEfectivaDiaria("Efectiva", bd("2"), "Mensual", 30));
        // Nominal anual capitalizable diariamente
        assertEquals(bd("0.0006667"), tasaEfectivaDiaria("Nominal", bd("24"), "Anual", 1));
        assertEquals(bd("0.0201945"), tasaEfectivaMensual("Nominal", bd("24"), "Anual", 1));
        // Nominal trimestral capitalizable mensualmente
        assertEquals(bd("0.0200000"), tasaEfectivaMensual("Nominal", bd("6"), "Trimestral", 30));
        // Efectiva semestral y efectiva diaria
        assertEquals(bd("0.0190676"), tasaEfectivaMensual("Efectiva", bd("12"), "Semestral", 30));
        assertEquals(bd("0.0304391"), tasaEfectivaMensual("Efectiva", bd("0.1"), "Diario", 30));
        // Con tasa efectiva, el periodo de capitalización no influye
        assertEquals(tasaEfectivaMensual("Efectiva", bd("12"), "Semestral", 30),
                tasaEfectivaMensual("Efectiva", bd("12"), "Semestral", 1));
    }

    @Test
    void conteoDeDiasYFechasEnCambioDeAnioYFebrero() {
        assertEquals(20, diasEntre(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 10)));
        assertEquals(0, diasEntre(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31)));
        // Compra después del corte de diciembre y día de pago menor al corte: pago en febrero del año siguiente
        assertEquals(LocalDate.of(2027, 2, 5), fechaPagoDelCiclo(LocalDate.of(2026, 12, 28), 25, 5));
        // Compra el mismo día de corte (hasta las 23:59:59): se paga en ese ciclo
        assertEquals(LocalDate.of(2026, 9, 30), fechaPagoDelCiclo(LocalDate.of(2026, 9, 25), 25, 30));
        // Cronograma que pasa por febrero con día de pago 30: vence el 28 y vuelve al 30 en marzo
        List<FilaCronograma> filas = cronogramaFrances(bd("300.00"), bd("0.0200000"), 3, LocalDate.of(2027, 1, 30), 30);
        assertEquals(LocalDate.of(2027, 2, 28), filas.get(1).fechaVencimiento());
        assertEquals(LocalDate.of(2027, 3, 30), filas.get(2).fechaVencimiento());
    }

    @Test
    void metodoFrancesConAjusteDeLaUltimaCuota() {
        List<FilaCronograma> filas = cronogramaFrances(bd("1000.00"), bd("0.03"), 6, LocalDate.of(2026, 11, 30), 30);
        assertEquals(bd("184.60"), filas.get(0).cuota());
        assertEquals(bd("845.40"), filas.get(0).saldoFinal());
        assertEquals(bd("5.38"), filas.get(5).interes());
        assertEquals(bd("179.20"), filas.get(5).amortizacion());
        // La última cuota se ajusta (184.58 en lugar de 184.60) para cerrar el saldo en 0.00
        assertEquals(bd("184.58"), filas.get(5).cuota());
        assertEquals(bd("0.00"), filas.get(5).saldoFinal());
        BigDecimal amortizado = filas.stream().map(FilaCronograma::amortizacion).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal intereses = filas.stream().map(FilaCronograma::interes).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(bd("1000.00"), amortizado);
        assertEquals(bd("107.58"), intereses);
        // Una sola cuota: capital más un periodo de interés
        assertEquals(bd("1030.00"), cuotaFrances(bd("1000.00"), bd("0.03"), 1));
    }

    @Test
    void casosSinInteres() {
        // Sin días de operación no hay interés; sin días de gracia el capital no cambia
        assertEquals(bd("0.00"), interes(bd("120.00"), bd("0.0007291"), 0));
        assertEquals(bd("450.00"), capitalActualizado(bd("450.00"), bd("0.0006603"), 0));
        assertEquals(bd("61.46"), valorPresente(bd("61.46"), bd("0.0007291"), 0));
        // Prepago de una compra a un solo pago: se descuenta con la TED por los días que se adelanta
        assertEquals(bd("1.0073150"), factor(bd("0.0007291"), 10));
        assertEquals(bd("61.01"), valorPresente(bd("61.46"), bd("0.0007291"), 10));
    }
}
