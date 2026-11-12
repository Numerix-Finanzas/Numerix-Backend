package com.upc.numerix.finanzas;

import com.upc.numerix.entities.Cliente;
import com.upc.numerix.entities.Cuota;
import com.upc.numerix.entities.Obligacion;
import com.upc.numerix.repositories.ObligacionRepositorio;
import com.upc.numerix.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static com.upc.numerix.finanzas.CalculadoraFinanciera.*;

/*
 Estado de la cuenta corriente de un cliente: crédito utilizado y disponible, deuda de cada cuota
 a una fecha (con mora o descuento por prepago) y estados de cuotas y deudas.
 En los métodos que reciben "alCorte": null usa el estado actual de las cuotas; una fecha considera
 pagadas solo las cuotas cuyo pago se realizó hasta esa fecha (consultas a una fecha pasada).
 */
@Component
public class CuentaCorriente {

    @Autowired
    private ObligacionRepositorio obligacionRepositorio;

    // Deuda de una cuota a una fecha, ya separada según el orden de prelación
    public record DeudaCuota(int diasMora, int diasAnticipacion, BigDecimal capital, BigDecimal interesCompensatorio,
                             BigDecimal interesMoratorio, BigDecimal total) {
    }

    // ---------- Tasas de la obligación (copia de las condiciones del cliente al comprar) ----------

    public BigDecimal tedCompensatoria(Obligacion o) {
        return tasaEfectivaDiaria(o.getTipoTasa(), o.getPorcentajeTasa(), o.getPeriodoTasa(), o.getPeriodoCapitalizacion());
    }

    public BigDecimal temCompensatoria(Obligacion o) {
        return tasaEfectivaMensual(o.getTipoTasa(), o.getPorcentajeTasa(), o.getPeriodoTasa(), o.getPeriodoCapitalizacion());
    }

    public BigDecimal tedMoratoria(Obligacion o) {
        return tasaEfectivaDiaria(o.getTipoTasaMoratoria(), o.getPorcentajeTasaMoratoria(), o.getPeriodoTasa(),
                o.getPeriodoCapitalizacion());
    }

    public boolean esCuotas(Obligacion o) {
        return Constantes.CUOTAS.equals(o.getCompra().getTipoPago());
    }

    /*
     Deuda de una cuota si se paga en "fechaPago":
     - Con atraso: IM = DV * ((1 + TEDm)^dm - 1), donde DV es la cuota (capital + interés compensatorio).
     - Antes del vencimiento (si descontarPrepago): C = DV / (1 + TEP)^d, con la TEM en las compras en
       cuotas (d en meses) y la TED en un solo pago (d en días); el interés imputado es C - capital.
     - En la fecha pactada: DV.
     */
    public DeudaCuota deudaCuota(Cuota cuota, LocalDate fechaPago, boolean descontarPrepago) {
        Obligacion o = cuota.getObligacion();
        BigDecimal deudaVencida = cuota.getMontoCuota();
        BigDecimal capital = cuota.getAmortizacion();
        BigDecimal interes = cuota.getInteresCompensatorio();
        int dias = diasEntre(cuota.getFechaVencimiento(), fechaPago);

        if (dias > 0) {
            BigDecimal mora = CalculadoraFinanciera.interes(deudaVencida, tedMoratoria(o), dias);
            return new DeudaCuota(dias, 0, capital, interes, mora, deudaVencida.add(mora));
        }
        if (dias < 0 && descontarPrepago) {
            int diasAnticipacion = -dias;
            BigDecimal total = esCuotas(o)
                    ? valorPresente(deudaVencida, temCompensatoria(o), (double) diasAnticipacion / DIAS_MES_COMERCIAL)
                    : valorPresente(deudaVencida, tedCompensatoria(o), diasAnticipacion);
            BigDecimal interesDescontado = total.subtract(capital).max(BigDecimal.ZERO);
            return new DeudaCuota(0, diasAnticipacion, total.subtract(interesDescontado), interesDescontado,
                    BigDecimal.ZERO.setScale(2), total);
        }
        return new DeudaCuota(0, 0, capital, interes, BigDecimal.ZERO.setScale(2), deudaVencida);
    }

    // ---------- Línea de crédito ----------

    public boolean pagada(Cuota cuota, LocalDate alCorte) {
        return alCorte == null ? cuota.isPagada() : cuota.pagadaAl(alCorte);
    }

    /*
     Capital de la compra que aún consume la línea de crédito. En cuotas se toma la proporción del
     capital actualizado que falta amortizar, aplicada al monto de la compra, para que la línea
     se libere en la misma medida en que se paga.
     */
    public BigDecimal capitalPendiente(Obligacion o, LocalDate alCorte) {
        BigDecimal pendiente = o.getCuotas().stream()
                .filter(c -> !pagada(c, alCorte))
                .map(Cuota::getAmortizacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pendiente.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return o.getCapitalInicial().multiply(pendiente)
                .divide(o.getCapitalActualizado(), 2, RoundingMode.HALF_UP);
    }

    public List<Obligacion> obligaciones(Cliente cliente) {
        return obligacionRepositorio
                .findByCompraClienteIdClienteOrderByCompraFechaCompraAscCompraIdCompraAsc(cliente.getIdCliente());
    }

    public BigDecimal creditoUtilizado(Cliente cliente, LocalDate alCorte) {
        return obligaciones(cliente).stream()
                .filter(o -> alCorte == null || !o.getCompra().getFechaCompra().isAfter(alCorte))
                .map(o -> capitalPendiente(o, alCorte))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    public BigDecimal creditoDisponible(Cliente cliente, LocalDate alCorte) {
        return redondear2(cliente.getCondicionCredito().getLimiteCredito().subtract(creditoUtilizado(cliente, alCorte)).max(BigDecimal.ZERO));
    }

    // deudaActual = capital pendiente + interés compensatorio + interés moratorio de las cuotas no pagadas
    public BigDecimal deudaActual(Cliente cliente, LocalDate fecha) {
        return obligaciones(cliente).stream()
                .flatMap(o -> o.getCuotas().stream())
                .filter(c -> !c.isPagada())
                .map(c -> deudaCuota(c, fecha, false).total())
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    // ---------- Estados ----------

    public String estadoCuota(Cuota cuota, LocalDate fecha, LocalDate alCorte) {
        if (pagada(cuota, alCorte)) {
            return Constantes.PAGADO;
        }
        return cuota.getFechaVencimiento().isBefore(fecha) ? Constantes.VENCIDO : Constantes.PENDIENTE;
    }

    public String estadoDeuda(Obligacion o, LocalDate fecha, LocalDate alCorte) {
        boolean todasPagadas = true;
        for (Cuota cuota : o.getCuotas()) {
            String estado = estadoCuota(cuota, fecha, alCorte);
            if (Constantes.VENCIDO.equals(estado)) {
                return Constantes.VENCIDO;
            }
            todasPagadas &= Constantes.PAGADO.equals(estado);
        }
        return todasPagadas ? Constantes.PAGADO : Constantes.PENDIENTE;
    }

    public String formatoMonto(String moneda, BigDecimal monto) {
        return Constantes.simboloMoneda(moneda) + " " + String.format(java.util.Locale.US, "%,.2f", monto);
    }
}
