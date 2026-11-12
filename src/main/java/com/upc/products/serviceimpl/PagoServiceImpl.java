package com.upc.numerix.serviceimpl;

import com.upc.numerix.dtos.ObligacionPendienteDTO;
import com.upc.numerix.dtos.PagoDTO;
import com.upc.numerix.dtos.PagoRequestDTO;
import com.upc.numerix.dtos.PagoSimulacionDTO;
import com.upc.numerix.entities.Cliente;
import com.upc.numerix.entities.Compra;
import com.upc.numerix.entities.Cuota;
import com.upc.numerix.entities.Obligacion;
import com.upc.numerix.entities.Pago;
import com.upc.numerix.exceptions.OperacionRechazadaException;
import com.upc.numerix.exceptions.RecursoNoEncontradoException;
import com.upc.numerix.finanzas.CuentaCorriente;
import com.upc.numerix.finanzas.CuentaCorriente.DeudaCuota;
import com.upc.numerix.repositories.CuotaRepositorio;
import com.upc.numerix.repositories.PagoRepositorio;
import com.upc.numerix.security.services.AccesoService;
import com.upc.numerix.services.PagoService;
import com.upc.numerix.util.Constantes;
import com.upc.numerix.util.GeneradorId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagoServiceImpl implements PagoService {

    // Diferencia máxima aceptada entre el monto pagado y el monto a pagar (medio céntimo)
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

    @Autowired
    private CuotaRepositorio cuotaRepositorio;
    @Autowired
    private PagoRepositorio pagoRepositorio;
    @Autowired
    private AccesoService accesoService;
    @Autowired
    private CuentaCorriente cuentaCorriente;

    // Cuotas no pagadas del cliente; en cada compra en cuotas solo se puede pagar la más antigua
    @Override
    public List<ObligacionPendienteDTO> listarPendientes(String idCliente) {
        Cliente cliente = accesoService.clienteAccesible(idCliente);
        LocalDate hoy = Constantes.hoy();
        return cuentaCorriente.obligaciones(cliente).stream()
                .flatMap(o -> o.getCuotas().stream().filter(c -> !c.isPagada()).limit(1))
                .map(c -> {
                    Compra compra = c.getObligacion().getCompra();
                    return ObligacionPendienteDTO.builder()
                            .idCuota(c.getIdCuota())
                            .idObligacion(c.getObligacion().getIdObligacion())
                            .idCompra(compra.getIdCompra())
                            .descripcionProducto(compra.getProducto().getDescripcionProducto())
                            .fechaCompra(compra.getFechaCompra())
                            .tipoPago(compra.getTipoPago())
                            .numeroPeriodo(c.getNumeroCuota())
                            .numeroCuotas(compra.getNumeroCuotas())
                            .fechaPagoPactada(c.getFechaVencimiento())
                            .deudaVencida(c.getMontoCuota())
                            .estadoCuota(cuentaCorriente.estadoCuota(c, hoy, null))
                            .build();
                })
                .toList();
    }

    @Override
    public PagoSimulacionDTO simularPago(Long idCuota, LocalDate fechaPagoReal) {
        Cuota cuota = cuotaPagable(idCuota, fechaPagoReal);
        LocalDate fecha = fechaPagoReal != null ? fechaPagoReal : Constantes.hoy();
        return simulacion(cuota, fecha, cuentaCorriente.deudaCuota(cuota, fecha, true));
    }

    /*
     No se aceptan pagos parciales: el monto pagado debe ser igual al monto total de la obligación.
     El pago se imputa en el orden de prelación: interés moratorio, interés compensatorio y capital.
     */
    @Transactional
    @Override
    public PagoDTO registrarPago(PagoRequestDTO request) {
        LocalDate fecha = request.getFechaPagoReal() != null ? request.getFechaPagoReal() : Constantes.hoy();
        Cuota cuota = cuotaPagable(request.getIdCuota(), fecha);
        Obligacion obligacion = cuota.getObligacion();
        DeudaCuota deuda = cuentaCorriente.deudaCuota(cuota, fecha, true);
        String moneda = obligacion.getMoneda();

        BigDecimal diferencia = request.getMontoPagado().subtract(deuda.total());
        if (diferencia.abs().compareTo(TOLERANCIA) >= 0) {
            String monto = cuentaCorriente.formatoMonto(moneda, deuda.total());
            throw new OperacionRechazadaException(diferencia.signum() < 0
                    ? "No se permiten pagos parciales. Monto a pagar: " + monto + "."
                    : "El monto pagado excede el monto a pagar: " + monto + ".");
        }

        Pago pago = new Pago();
        pago.setIdPago(GeneradorId.siguiente("PAG-", 6, pagoRepositorio));
        pago.setObligacion(obligacion);
        pago.setCuota(cuota);
        pago.setFechaPagoReal(fecha);
        pago.setMontoPagado(deuda.total());
        pago.setMontoImputadoMora(deuda.interesMoratorio());
        pago.setMontoImputadoInteres(deuda.interesCompensatorio());
        pago.setMontoImputadoCapital(deuda.capital());
        pago.setDiasMora(deuda.diasMora());
        pago.setDiasAnticipacion(deuda.diasAnticipacion());
        pago.setFechaRegistro(LocalDateTime.now(Constantes.ZONA));
        pago = pagoRepositorio.save(pago);

        cuota.setPagada(true);
        cuota.setFechaPago(fecha);
        cuotaRepositorio.save(cuota);

        Cliente cliente = obligacion.getCompra().getCliente();
        PagoDTO dto = aDTO(pago);
        dto.setEstadoOperacion(Constantes.PROCESADA);
        dto.setEstadoDeuda(cuentaCorriente.estadoDeuda(obligacion, Constantes.hoy(), null));
        dto.setCreditoDisponible(cuentaCorriente.creditoDisponible(cliente, null));
        dto.setMensaje("Pago registrado." + (deuda.diasAnticipacion() > 0
                ? " Pago anticipado: se descontaron " + deuda.diasAnticipacion() + " días de intereses." : ""));
        return dto;
    }

    // Busca la cuota, valida el acceso y que sea la siguiente cuota pendiente de su compra
    private Cuota cuotaPagable(Long idCuota, LocalDate fechaPagoReal) {
        accesoService.negocioActual();
        Cuota cuota = cuotaRepositorio.findById(idCuota)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obligación no encontrada: " + idCuota));
        Obligacion obligacion = cuota.getObligacion();
        accesoService.clienteAccesible(obligacion.getCompra().getCliente().getIdCliente());
        if (cuota.isPagada()) {
            throw new OperacionRechazadaException("La cuota ya se encuentra pagada.");
        }
        Cuota siguiente = obligacion.getCuotas().stream().filter(c -> !c.isPagada()).findFirst().orElse(cuota);
        if (!siguiente.getIdCuota().equals(cuota.getIdCuota())) {
            throw new OperacionRechazadaException("Debe pagar primero la cuota N.º " + siguiente.getNumeroCuota() + ".");
        }
        if (fechaPagoReal != null && fechaPagoReal.isBefore(obligacion.getCompra().getFechaCompra())) {
            throw new OperacionRechazadaException("La fecha de pago no puede ser anterior a la fecha de compra.");
        }
        return cuota;
    }

    private PagoSimulacionDTO simulacion(Cuota cuota, LocalDate fecha, DeudaCuota deuda) {
        Obligacion o = cuota.getObligacion();
        return PagoSimulacionDTO.builder()
                .idCuota(cuota.getIdCuota())
                .idObligacion(o.getIdObligacion())
                .idCompra(o.getCompra().getIdCompra())
                .descripcionProducto(o.getCompra().getProducto().getDescripcionProducto())
                .numeroPeriodo(cuota.getNumeroCuota())
                .moneda(o.getMoneda())
                .fechaPagoPactada(cuota.getFechaVencimiento())
                .fechaPagoReal(fecha)
                .diasMora(deuda.diasMora())
                .diasAnticipacion(deuda.diasAnticipacion())
                .deudaVencida(cuota.getMontoCuota())
                .capitalPendienteDeuda(deuda.capital())
                .interesCompensatorioDeuda(deuda.interesCompensatorio())
                .interesMoratorioObligacion(deuda.interesMoratorio())
                .montoTotalObligacion(deuda.total())
                .ordenPrelacion(Constantes.ORDEN_PRELACION)
                .build();
    }

    static PagoDTO aDTO(Pago pago) {
        return PagoDTO.builder()
                .idPago(pago.getIdPago())
                .idObligacion(pago.getObligacion().getIdObligacion())
                .idCompra(pago.getObligacion().getCompra().getIdCompra())
                .idCuota(pago.getCuota().getIdCuota())
                .numeroPeriodo(pago.getCuota().getNumeroCuota())
                .descripcionProducto(pago.getObligacion().getCompra().getProducto().getDescripcionProducto())
                .fechaPagoReal(pago.getFechaPagoReal())
                .diasMora(pago.getDiasMora())
                .diasAnticipacion(pago.getDiasAnticipacion())
                .montoImputadoMora(pago.getMontoImputadoMora())
                .montoImputadoInteres(pago.getMontoImputadoInteres())
                .montoImputadoCapital(pago.getMontoImputadoCapital())
                .montoPagado(pago.getMontoPagado())
                .build();
    }
}
