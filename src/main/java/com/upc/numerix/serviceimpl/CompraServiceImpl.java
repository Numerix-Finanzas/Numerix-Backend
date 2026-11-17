package com.upc.numerix.serviceimpl;

import com.upc.numerix.dtos.CompraRequestDTO;
import com.upc.numerix.dtos.CompraResultadoDTO;
import com.upc.numerix.dtos.DetalleCompraDTO;
import com.upc.numerix.dtos.DetalleCompraRequestDTO;
import com.upc.numerix.entities.*;
import com.upc.numerix.exceptions.OperacionRechazadaException;
import com.upc.numerix.exceptions.RecursoNoEncontradoException;
import com.upc.numerix.finanzas.CalculadoraFinanciera.FilaCronograma;
import com.upc.numerix.finanzas.CuentaCorriente;
import com.upc.numerix.repositories.CompraRepositorio;
import com.upc.numerix.repositories.ObligacionRepositorio;
import com.upc.numerix.repositories.ProductoRepositorio;
import com.upc.numerix.security.services.AccesoService;
import com.upc.numerix.services.CompraService;
import com.upc.numerix.util.Constantes;
import com.upc.numerix.util.GeneradorId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.upc.numerix.finanzas.CalculadoraFinanciera.*;

@Service
public class CompraServiceImpl implements CompraService {

    @Autowired
    private CompraRepositorio compraRepositorio;
    @Autowired
    private ObligacionRepositorio obligacionRepositorio;
    @Autowired
    private ProductoRepositorio productoRepositorio;
    @Autowired
    private AccesoService accesoService;
    @Autowired
    private CuentaCorriente cuentaCorriente;

    // Datos calculados de una compra antes de registrarla
    private record PlanCompra(Cliente cliente, List<DetalleCompra> detalles, LocalDate fechaCompra, int numeroCuotas,
                              BigDecimal montoCompra, BigDecimal capitalActualizado, int diasGracia,
                              List<FilaCronograma> cuotas, CompraResultadoDTO resultado) {
    }

    // Resumen previo al registro (pantalla Registro de compra a crédito)
    @Override
    public CompraResultadoDTO simularCompra(CompraRequestDTO request) {
        PlanCompra plan = calcular(request);
        plan.resultado().setMensaje("La compra cumple las condiciones de crédito del cliente.");
        return plan.resultado();
    }

    @Transactional
    @Override
    public CompraResultadoDTO registrarCompra(CompraRequestDTO request) {
        PlanCompra plan = calcular(request);
        LocalDateTime ahora = LocalDateTime.now(Constantes.ZONA);
        Cliente cliente = plan.cliente();
        CondicionCredito condicion = cliente.getCondicionCredito();

        Compra compra = new Compra();
        compra.setIdCompra(GeneradorId.siguiente("CMP-", 6, compraRepositorio));
        compra.setCliente(cliente);
        compra.setFechaCompra(plan.fechaCompra());
        compra.setTipoPago(request.getTipoPago());
        compra.setNumeroCuotas(plan.numeroCuotas());
        compra.setMontoCompra(plan.montoCompra());
        compra.setFechaRegistro(ahora);
        for (DetalleCompra detalle : plan.detalles()) {
            detalle.setCompra(compra);
            compra.getDetalles().add(detalle);
        }
        compra = compraRepositorio.save(compra);

        // La obligación guarda una copia de las condiciones de tasa vigentes del cliente
        Obligacion obligacion = new Obligacion();
        obligacion.setIdObligacion(GeneradorId.siguiente("OBL-", 6, obligacionRepositorio));
        obligacion.setCompra(compra);
        obligacion.setMoneda(condicion.getMoneda());
        obligacion.setTipoTasa(condicion.getTipoTasa());
        obligacion.setPorcentajeTasa(condicion.getPorcentajeTasa());
        obligacion.setPeriodoTasa(condicion.getPeriodoTasa());
        obligacion.setPeriodoCapitalizacion(condicion.getPeriodoCapitalizacion());
        obligacion.setTipoTasaMoratoria(condicion.getTipoTasaMoratoria());
        obligacion.setPorcentajeTasaMoratoria(condicion.getPorcentajeTasaMoratoria());
        obligacion.setCapitalInicial(plan.montoCompra());
        obligacion.setCapitalActualizado(plan.capitalActualizado());
        obligacion.setDiasGracia(plan.diasGracia());
        obligacion.setFechaRegistro(ahora);
        for (FilaCronograma fila : plan.cuotas()) {
            Cuota cuota = new Cuota();
            cuota.setObligacion(obligacion);
            cuota.setNumeroCuota(fila.numeroCuota());
            cuota.setFechaVencimiento(fila.fechaVencimiento());
            cuota.setSaldoInicial(fila.saldoInicial());
            cuota.setInteresCompensatorio(fila.interes());
            cuota.setAmortizacion(fila.amortizacion());
            cuota.setMontoCuota(fila.cuota());
            cuota.setSaldoFinal(fila.saldoFinal());
            cuota.setPagada(false);
            obligacion.getCuotas().add(cuota);
        }
        obligacion = obligacionRepositorio.save(obligacion);
        compra.setObligacion(obligacion);

        CompraResultadoDTO resultado = plan.resultado();
        BigDecimal disponibleDespues = redondear2(resultado.getCreditoDisponible().subtract(plan.montoCompra()));
        resultado.setIdCompra(compra.getIdCompra());
        resultado.setIdObligacion(obligacion.getIdObligacion());
        resultado.setCreditoDisponibleDespues(disponibleDespues);
        resultado.setMensaje("Compra registrada. Crédito disponible: "
                + cuentaCorriente.formatoMonto(condicion.getMoneda(), disponibleDespues) + ".");
        return resultado;
    }

    /*
     Arma las líneas de la compra. Cada producto debe ser del negocio, estar activo, no repetirse y
     admitir el tipo de pago elegido para la compra (su modalidad de venta).
     */
    private List<DetalleCompra> armarDetalles(CompraRequestDTO request, Negocio negocio, boolean enCuotas) {
        List<DetalleCompraRequestDTO> lineas = request.lineas();
        if (lineas.isEmpty()) {
            throw new OperacionRechazadaException("Agregue al menos un producto a la compra.");
        }
        Set<String> vistos = new HashSet<>();
        List<DetalleCompra> detalles = new ArrayList<>();
        for (DetalleCompraRequestDTO linea : lineas) {
            Producto producto = productoRepositorio.findById(linea.getIdProducto())
                    .filter(p -> p.getNegocio().getIdNegocio().equals(negocio.getIdNegocio()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + linea.getIdProducto()));
            String nombre = producto.getDescripcionProducto();
            if (!vistos.add(producto.getIdProducto())) {
                throw new OperacionRechazadaException("El producto " + nombre
                        + " está repetido en la compra: indique la cantidad total en una sola línea.");
            }
            if (!Constantes.ACTIVO.equals(producto.getEstadoProducto())) {
                throw new OperacionRechazadaException("El producto " + nombre + " se encuentra inactivo y no puede venderse.");
            }
            String modalidad = producto.getModalidadVentaProducto();
            if (enCuotas && Constantes.MODALIDAD_CONTADO.equals(modalidad)) {
                throw new OperacionRechazadaException("El producto " + nombre + " solo se vende a un solo pago.");
            }
            if (!enCuotas && Constantes.MODALIDAD_CREDITO.equals(modalidad)) {
                throw new OperacionRechazadaException("El producto " + nombre + " solo se vende en cuotas.");
            }
            DetalleCompra detalle = new DetalleCompra();
            detalle.setProducto(producto);
            detalle.setCantidadProducto(linea.getCantidadProducto());
            detalle.setPrecioCreditoAplicado(producto.getPrecioCredito());
            detalle.setSubtotal(redondear2(producto.getPrecioCredito().multiply(BigDecimal.valueOf(linea.getCantidadProducto()))));
            detalles.add(detalle);
        }
        return detalles;
    }

    private PlanCompra calcular(CompraRequestDTO request) {
        Negocio negocio = accesoService.negocioActual();
        Cliente cliente = accesoService.clienteAccesible(request.getIdCliente());
        CondicionCredito condicion = cliente.getCondicionCredito();

        // Validaciones (juego de prueba 3): ninguna compra rechazada se registra
        if (!Constantes.ACTIVO.equals(cliente.getEstadoCliente())) {
            throw new OperacionRechazadaException("El cliente se encuentra inactivo y no puede registrar nuevas compras.");
        }
        boolean enCuotas = Constantes.CUOTAS.equals(request.getTipoPago());
        List<DetalleCompra> detalles = armarDetalles(request, negocio, enCuotas);

        int numeroCuotas = enCuotas ? request.getNumeroCuotas() : 1;
        LocalDate fechaCompra = request.getFechaCompra() != null ? request.getFechaCompra() : Constantes.hoy();
        // montoCompra = Σ cantidadProducto × precioCredito de todas las líneas
        BigDecimal montoCompra = detalles.stream().map(DetalleCompra::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditoDisponible = cuentaCorriente.creditoDisponible(cliente, null);
        String moneda = condicion.getMoneda();

        if (montoCompra.compareTo(creditoDisponible) > 0) {
            throw new OperacionRechazadaException("El monto de la compra excede el crédito disponible ("
                    + cuentaCorriente.formatoMonto(moneda, creditoDisponible) + ").");
        }
        if (numeroCuotas > condicion.getMaximoMesesCredito()) {
            throw new OperacionRechazadaException("El número de cuotas supera el máximo permitido ("
                    + condicion.getMaximoMesesCredito() + ").");
        }

        BigDecimal ted = tasaEfectivaDiaria(condicion.getTipoTasa(), condicion.getPorcentajeTasa(),
                condicion.getPeriodoTasa(), condicion.getPeriodoCapitalizacion());
        BigDecimal tem = tasaEfectivaMensual(condicion.getTipoTasa(), condicion.getPorcentajeTasa(),
                condicion.getPeriodoTasa(), condicion.getPeriodoCapitalizacion());
        // Fecha de pago del ciclo: compras hasta el día de corte se pagan ese mes; posteriores, el siguiente
        LocalDate fechaPagoCiclo = fechaPagoDelCiclo(fechaCompra, condicion.getDiaCorte(), condicion.getDiaPagoPactado());

        CompraResultadoDTO resultado = CompraResultadoDTO.builder()
                .estadoOperacion(Constantes.PROCESADA)
                .idCliente(cliente.getIdCliente())
                .nombreCliente(cliente.getNombreCliente())
                .descripcionProducto(detalles.stream().map(d -> d.getProducto().getDescripcionProducto())
                        .collect(Collectors.joining(", ")))
                .moneda(moneda)
                .fechaCompra(fechaCompra)
                .cantidadProducto(detalles.stream().mapToInt(DetalleCompra::getCantidadProducto).sum())
                .detalles(detalles.stream().map(CompraServiceImpl::aDTO).toList())
                .montoCompra(montoCompra)
                .tipoPago(request.getTipoPago())
                .numeroCuotas(numeroCuotas)
                .limiteCredito(condicion.getLimiteCredito())
                .deudaActual(cuentaCorriente.deudaActual(cliente, fechaCompra))
                .creditoDisponible(creditoDisponible)
                .creditoDisponibleDespues(redondear2(creditoDisponible.subtract(montoCompra)))
                .tedCompensatoria(ted)
                .temCompensatoria(tem)
                .build();

        if (!enCuotas) {
            // Un solo pago: IC = C * ((1 + TED)^d - 1), d = días entre la compra y la fecha de pago pactada
            int diasOperacion = diasEntre(fechaCompra, fechaPagoCiclo);
            BigDecimal interes = interes(montoCompra, ted, diasOperacion);
            BigDecimal total = montoCompra.add(interes);
            resultado.setFechaPagoPactada(fechaPagoCiclo);
            resultado.setProximaFechaPago(fechaPagoCiclo);
            resultado.setDiasOperacion(diasOperacion);
            resultado.setInteresCompensatorioDeuda(interes);
            resultado.setMontoTotalPagar(total);
            resultado.setDiasGracia(0);
            resultado.setCapitalActualizado(montoCompra);
            List<FilaCronograma> unica = List.of(new FilaCronograma(1, fechaPagoCiclo, montoCompra, interes,
                    montoCompra, total, BigDecimal.ZERO.setScale(2)));
            return new PlanCompra(cliente, detalles, fechaCompra, 1, montoCompra, montoCompra, 0, unica, resultado);
        }

        /*
         Cuotas: el primer periodo empieza en la fecha de pago del ciclo y la primera cuota vence un mes
         después. Los días entre la compra y ese inicio son de gracia total y se capitalizan con la TED:
         CA = C * (1 + TED)^dg. La cuota se calcula con el método francés vencido ordinario sobre CA.
         */
        int diasGracia = Math.max(0, diasEntre(fechaCompra, fechaPagoCiclo));
        BigDecimal capitalActualizado = capitalActualizado(montoCompra, ted, diasGracia);
        LocalDate primeraCuota = conDia(YearMonth.from(fechaPagoCiclo).plusMonths(1), condicion.getDiaPagoPactado());
        List<FilaCronograma> cuotas = cronogramaFrances(capitalActualizado, tem, numeroCuotas, primeraCuota,
                condicion.getDiaPagoPactado());
        resultado.setDiasGracia(diasGracia);
        resultado.setFactorCapitalizacion(factor(ted, diasGracia));
        resultado.setCapitalActualizado(capitalActualizado);
        resultado.setMontoCuota(cuotas.get(0).cuota());
        resultado.setFechaPagoPactada(primeraCuota);
        resultado.setProximaFechaPago(primeraCuota);
        resultado.setMontoTotalPagar(cuotas.stream().map(FilaCronograma::cuota).reduce(BigDecimal.ZERO, BigDecimal::add));
        // Costo financiero total: intereses de la gracia más los del cronograma
        resultado.setInteresCompensatorioDeuda(resultado.getMontoTotalPagar().subtract(montoCompra));
        return new PlanCompra(cliente, detalles, fechaCompra, numeroCuotas, montoCompra, capitalActualizado,
                diasGracia, cuotas, resultado);
    }

    static DetalleCompraDTO aDTO(DetalleCompra d) {
        return new DetalleCompraDTO(d.getProducto().getIdProducto(), d.getProducto().getDescripcionProducto(),
                d.getCantidadProducto(), d.getPrecioCreditoAplicado(), d.getSubtotal());
    }
}
