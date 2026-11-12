package com.upc.numerix.serviceimpl;

import com.upc.numerix.dtos.*;
import com.upc.numerix.entities.Cliente;
import com.upc.numerix.entities.Compra;
import com.upc.numerix.entities.CondicionCredito;
import com.upc.numerix.entities.Cuota;
import com.upc.numerix.entities.Obligacion;
import com.upc.numerix.exceptions.OperacionRechazadaException;
import com.upc.numerix.exceptions.RecursoNoEncontradoException;
import com.upc.numerix.finanzas.CuentaCorriente;
import com.upc.numerix.finanzas.CuentaCorriente.DeudaCuota;
import com.upc.numerix.repositories.CompraRepositorio;
import com.upc.numerix.repositories.PagoRepositorio;
import com.upc.numerix.security.services.AccesoService;
import com.upc.numerix.services.ConsultaService;
import com.upc.numerix.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.upc.numerix.finanzas.CalculadoraFinanciera.factor;
import static com.upc.numerix.finanzas.CalculadoraFinanciera.fechaPagoDelCiclo;
import static com.upc.numerix.finanzas.CalculadoraFinanciera.redondear2;

@Service
@Transactional(readOnly = true)
public class ConsultaServiceImpl implements ConsultaService {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate FECHA_MAXIMA = LocalDate.of(9999, 12, 31);

    @Autowired
    private CompraRepositorio compraRepositorio;
    @Autowired
    private PagoRepositorio pagoRepositorio;
    @Autowired
    private AccesoService accesoService;
    @Autowired
    private CuentaCorriente cuentaCorriente;

    // El cronograma de pagos solo existe para las compras en cuotas
    @Override
    public CronogramaDTO cronograma(String idCompra) {
        Compra compra = compraRepositorio.findById(idCompra)
                .orElseThrow(() -> new RecursoNoEncontradoException("Compra no encontrada: " + idCompra));
        Cliente cliente = accesoService.clienteAccesible(compra.getCliente().getIdCliente());
        if (!Constantes.CUOTAS.equals(compra.getTipoPago())) {
            throw new OperacionRechazadaException("La compra " + idCompra
                    + " es a un solo pago: no tiene cronograma de pagos.");
        }
        return armarCronograma(compra, cliente);
    }

    // Pantalla Cronograma: cronogramas de todas las compras en cuotas del cliente
    @Override
    public CronogramaClienteDTO cronogramaCliente(String idCliente) {
        Cliente cliente = accesoService.clienteAccesible(idCliente);
        List<CronogramaDTO> cronogramas = compraRepositorio
                .findByClienteIdClienteOrderByFechaCompraAscIdCompraAsc(cliente.getIdCliente()).stream()
                .filter(compra -> Constantes.CUOTAS.equals(compra.getTipoPago()))
                .map(compra -> armarCronograma(compra, cliente))
                .toList();
        return CronogramaClienteDTO.builder()
                .idCliente(cliente.getIdCliente())
                .nombreCliente(cliente.getNombreCliente())
                .proximaFechaPago(cronogramas.stream().map(CronogramaDTO::getProximaFechaPago)
                        .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null))
                .cronogramas(cronogramas)
                .build();
    }

    private CronogramaDTO armarCronograma(Compra compra, Cliente cliente) {
        Obligacion o = compra.getObligacion();
        LocalDate hoy = Constantes.hoy();

        List<CuotaDTO> cuotas = o.getCuotas().stream().map(c -> CuotaDTO.builder()
                .idCuota(c.getIdCuota())
                .numeroPeriodo(c.getNumeroCuota())
                .fechaVencimientoPeriodo(c.getFechaVencimiento())
                .saldoInicialPeriodo(c.getSaldoInicial())
                .interesPeriodo(c.getInteresCompensatorio())
                .amortizacionPeriodo(c.getAmortizacion())
                .cuotaPeriodo(c.getMontoCuota())
                .saldoFinalPeriodo(c.getSaldoFinal())
                .estadoCuota(cuentaCorriente.estadoCuota(c, hoy, null))
                .fechaPagoReal(c.getFechaPago())
                .build()).toList();

        return CronogramaDTO.builder()
                .idCompra(compra.getIdCompra())
                .idObligacion(o.getIdObligacion())
                .idCliente(cliente.getIdCliente())
                .nombreCliente(cliente.getNombreCliente())
                .descripcionProducto(compra.getProducto().getDescripcionProducto())
                .moneda(o.getMoneda())
                .fechaCompra(compra.getFechaCompra())
                .montoCompra(compra.getMontoCompra())
                .tipoPago(compra.getTipoPago())
                .numeroCuotas(compra.getNumeroCuotas())
                .totalPeriodosAmortizacion(o.getCuotas().size())
                .tedCompensatoria(cuentaCorriente.tedCompensatoria(o))
                .temCompensatoria(cuentaCorriente.temCompensatoria(o))
                .diasGracia(o.getDiasGracia())
                .factorCapitalizacion(factor(cuentaCorriente.tedCompensatoria(o), o.getDiasGracia()))
                .capitalActualizado(o.getCapitalActualizado())
                .montoCuota(o.getCuotas().get(0).getMontoCuota())
                .cuotasPagadas((int) o.getCuotas().stream().filter(Cuota::isPagada).count())
                .totalInteres(suma(cuotas.stream().map(CuotaDTO::getInteresPeriodo).toList()))
                .totalAmortizacion(suma(cuotas.stream().map(CuotaDTO::getAmortizacionPeriodo).toList()))
                .totalCuotas(suma(cuotas.stream().map(CuotaDTO::getCuotaPeriodo).toList()))
                .proximaFechaPago(o.getCuotas().stream().filter(c -> !c.isPagada())
                        .map(Cuota::getFechaVencimiento).findFirst().orElse(null))
                .estadoDeuda(cuentaCorriente.estadoDeuda(o, hoy, null))
                .cuotas(cuotas)
                .build();
    }

    /*
     Reporte de deuda a una fecha de consulta (por defecto, hoy). Incluye las cuotas no pagadas que
     vencen hasta la próxima fecha de pago del ciclo, ordenadas por fecha de compra. Las ya vencidas
     muestran el interés moratorio por los días de atraso hasta la fecha de consulta, como ítem separado.
     */
    @Override
    public ReporteDeudaDTO reporteDeuda(String idCliente, LocalDate fechaConsulta) {
        Cliente cliente = accesoService.clienteAccesible(idCliente);
        CondicionCredito condicion = cliente.getCondicionCredito();
        LocalDate fecha = fechaConsulta != null ? fechaConsulta : Constantes.hoy();
        // Consulta a una fecha indicada: solo cuentan los pagos realizados hasta esa fecha
        LocalDate alCorte = fechaConsulta != null ? fechaConsulta : null;
        LocalDate proximaFechaPago = fechaPagoDelCiclo(fecha, condicion.getDiaCorte(), condicion.getDiaPagoPactado());

        List<DeudaDTO> deudas = new ArrayList<>();
        for (Obligacion o : cuentaCorriente.obligaciones(cliente)) {
            Compra compra = o.getCompra();
            if (compra.getFechaCompra().isAfter(fecha)) {
                continue;
            }
            for (Cuota c : o.getCuotas()) {
                if (cuentaCorriente.pagada(c, alCorte) || c.getFechaVencimiento().isAfter(proximaFechaPago)) {
                    continue;
                }
                DeudaCuota deuda = cuentaCorriente.deudaCuota(c, fecha, false);
                deudas.add(DeudaDTO.builder()
                        .fechaCompra(compra.getFechaCompra())
                        .idCompra(compra.getIdCompra())
                        .idObligacion(o.getIdObligacion())
                        .idCuota(c.getIdCuota())
                        .numeroPeriodo(c.getNumeroCuota())
                        .numeroCuotas(compra.getNumeroCuotas())
                        .descripcionProducto(compra.getProducto().getDescripcionProducto())
                        .fechaPagoPactada(c.getFechaVencimiento())
                        .diasMora(deuda.diasMora())
                        .capitalPendienteDeuda(deuda.capital())
                        .interesCompensatorioDeuda(deuda.interesCompensatorio())
                        .interesMoratorioDeuda(deuda.interesMoratorio())
                        .montoTotalDeuda(deuda.total())
                        .estadoDeuda(deuda.diasMora() > 0 ? Constantes.VENCIDO : Constantes.PENDIENTE)
                        .build());
            }
        }

        BigDecimal capitalTotal = suma(deudas.stream().map(DeudaDTO::getCapitalPendienteDeuda).toList());
        BigDecimal interesTotal = suma(deudas.stream().map(DeudaDTO::getInteresCompensatorioDeuda).toList());
        BigDecimal moraTotal = suma(deudas.stream().map(DeudaDTO::getInteresMoratorioDeuda).toList());
        BigDecimal utilizado = cuentaCorriente.creditoUtilizado(cliente, alCorte);
        // proximaFechaPago es nula si el cliente no tiene pagos pendientes
        boolean hayPendientes = cuentaCorriente.obligaciones(cliente).stream()
                .filter(o -> !o.getCompra().getFechaCompra().isAfter(fecha))
                .flatMap(o -> o.getCuotas().stream())
                .anyMatch(c -> !cuentaCorriente.pagada(c, alCorte));

        return ReporteDeudaDTO.builder()
                .idCliente(cliente.getIdCliente())
                .numeroDocumentoCliente(cliente.getNumeroDocumentoCliente())
                .nombreCliente(cliente.getNombreCliente())
                .moneda(condicion.getMoneda())
                .limiteCredito(condicion.getLimiteCredito())
                .creditoUtilizado(utilizado)
                .creditoDisponible(redondear2(condicion.getLimiteCredito().subtract(utilizado).max(BigDecimal.ZERO)))
                .fechaConsulta(fecha)
                .proximaFechaPago(hayPendientes ? proximaFechaPago : null)
                .diaCorte(condicion.getDiaCorte())
                .diaPagoPactado(condicion.getDiaPagoPactado())
                .cantidadObligacionesPendientes(deudas.size())
                .deudas(deudas)
                .capitalTotal(capitalTotal)
                .interesCompensatorioTotal(interesTotal)
                .interesMoratorioTotal(moraTotal)
                .montoTotalPagar(capitalTotal.add(interesTotal).add(moraTotal))
                .build();
    }

    @Override
    public List<CompraDTO> movimientosCompras(String idCliente, LocalDate fechaInicioConsulta, LocalDate fechaFinConsulta) {
        Cliente cliente = accesoService.clienteAccesible(idCliente);
        validarRango(fechaInicioConsulta, fechaFinConsulta);
        LocalDate hoy = Constantes.hoy();
        return compraRepositorio.findByClienteIdClienteAndFechaCompraBetweenOrderByFechaCompraAscIdCompraAsc(
                        cliente.getIdCliente(), fechaInicioConsulta != null ? fechaInicioConsulta : FECHA_MINIMA,
                        fechaFinConsulta != null ? fechaFinConsulta : FECHA_MAXIMA)
                .stream().map(compra -> CompraDTO.builder()
                        .idCompra(compra.getIdCompra())
                        .idObligacion(compra.getObligacion().getIdObligacion())
                        .fechaCompra(compra.getFechaCompra())
                        .descripcionProducto(compra.getProducto().getDescripcionProducto())
                        .cantidadProducto(compra.getCantidadProducto())
                        .tipoPago(compra.getTipoPago())
                        .numeroCuotas(compra.getNumeroCuotas())
                        .montoCompra(compra.getMontoCompra())
                        .estadoDeuda(cuentaCorriente.estadoDeuda(compra.getObligacion(), hoy, null))
                        .build())
                .toList();
    }

    @Override
    public List<PagoDTO> movimientosPagos(String idCliente, LocalDate fechaInicioConsulta, LocalDate fechaFinConsulta) {
        Cliente cliente = accesoService.clienteAccesible(idCliente);
        validarRango(fechaInicioConsulta, fechaFinConsulta);
        return pagoRepositorio.findByObligacionCompraClienteIdClienteAndFechaPagoRealBetweenOrderByFechaPagoRealAscIdPagoAsc(
                        cliente.getIdCliente(), fechaInicioConsulta != null ? fechaInicioConsulta : FECHA_MINIMA,
                        fechaFinConsulta != null ? fechaFinConsulta : FECHA_MAXIMA)
                .stream().map(PagoServiceImpl::aDTO).toList();
    }

    private void validarRango(LocalDate fechaInicioConsulta, LocalDate fechaFinConsulta) {
        if (fechaInicioConsulta != null && fechaFinConsulta != null && fechaInicioConsulta.isAfter(fechaFinConsulta)) {
            throw new OperacionRechazadaException("La fecha inicial debe ser menor o igual a la fecha final.");
        }
    }

    private BigDecimal suma(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
