package com.upc.numerix.serviceimpl;

import com.upc.numerix.dtos.ClienteDTO;
import com.upc.numerix.dtos.ClienteRegistradoDTO;
import com.upc.numerix.dtos.ClienteRequestDTO;
import com.upc.numerix.entities.Cliente;
import com.upc.numerix.entities.CondicionCredito;
import com.upc.numerix.entities.Negocio;
import com.upc.numerix.exceptions.OperacionRechazadaException;
import com.upc.numerix.finanzas.CalculadoraFinanciera;
import com.upc.numerix.finanzas.CuentaCorriente;
import com.upc.numerix.repositories.ClienteRepositorio;
import com.upc.numerix.security.entities.Usuario;
import com.upc.numerix.security.entities.Rol;
import com.upc.numerix.security.repositories.RolRepositorio;
import com.upc.numerix.security.repositories.UsuarioRepositorio;
import com.upc.numerix.security.services.AccesoService;
import com.upc.numerix.services.ClienteService;
import com.upc.numerix.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepositorio clienteRepositorio;
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private RolRepositorio rolRepositorio;
    @Autowired
    private AccesoService accesoService;
    @Autowired
    private CuentaCorriente cuentaCorriente;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<ClienteDTO> listarClientes() {
        Negocio negocio = accesoService.negocioActual();
        return clienteRepositorio.findByNegocioIdNegocioOrderByNombreClienteAsc(negocio.getIdNegocio())
                .stream().map(this::aDTO).toList();
    }

    @Override
    public ClienteDTO buscarCliente(String idCliente) {
        return aDTO(accesoService.clienteAccesible(idCliente));
    }

    /*
     Registra al cliente con sus condiciones de crédito y genera sus credenciales:
     usuario = número de documento, contraseña = "NX" + número de documento.
     */
    @Transactional
    @Override
    public ClienteRegistradoDTO registrarCliente(ClienteRequestDTO request) {
        Negocio negocio = accesoService.negocioActual();
        String documento = request.getNumeroDocumentoCliente().trim().toUpperCase();
        validarDocumento(request.getTipoDocumentoCliente(), documento);
        validarCondiciones(request);
        if (clienteRepositorio.existsByNumeroDocumentoCliente(documento) || usuarioRepositorio.existsByUsuario(documento)) {
            throw new OperacionRechazadaException("Ya existe un cliente registrado con el documento " + documento + ".");
        }

        Cliente cliente = new Cliente();
        cliente.setIdCliente("CLI-" + documento);
        cliente.setNegocio(negocio);
        cliente.setTipoDocumentoCliente(request.getTipoDocumentoCliente());
        cliente.setNumeroDocumentoCliente(documento);
        cliente.setEstadoCliente(Constantes.ACTIVO);
        cliente.setFechaRegistro(LocalDateTime.now(Constantes.ZONA));
        copiarCondiciones(request, cliente);
        cliente = clienteRepositorio.save(cliente);

        String contrasena = "NX" + documento;
        Usuario usuario = new Usuario();
        usuario.setUsuario(documento);
        usuario.setContrasena(passwordEncoder.encode(contrasena));
        usuario.setRol(rol(Constantes.ROL_CLIENTE));
        usuario.setNombreCompleto(cliente.getNombreCliente());
        usuario.setNegocio(negocio);
        usuario.setCliente(cliente);
        usuario.setActivo(true);
        usuario.setFechaRegistro(LocalDateTime.now(Constantes.ZONA));
        usuarioRepositorio.save(usuario);

        return new ClienteRegistradoDTO("¡Cliente registrado correctamente! Usuario y contraseña generados para el acceso del cliente.",
                aDTO(cliente), documento, contrasena);
    }

    // Las nuevas condiciones solo aplican a las compras futuras: cada obligación guarda las suyas
    @Transactional
    @Override
    public ClienteDTO modificarCondiciones(String idCliente, ClienteRequestDTO request) {
        Cliente cliente = clienteDelNegocio(idCliente);
        validarCondiciones(request);
        copiarCondiciones(request, cliente);
        return aDTO(clienteRepositorio.save(cliente));
    }

    @Transactional
    @Override
    public ClienteDTO cambiarEstado(String idCliente, String estado) {
        Cliente cliente = clienteDelNegocio(idCliente);
        cliente.setEstadoCliente(estado);
        return aDTO(clienteRepositorio.save(cliente));
    }

    private Cliente clienteDelNegocio(String idCliente) {
        accesoService.negocioActual();
        return accesoService.clienteAccesible(idCliente);
    }

    // DNI: 8 dígitos, CE: 9 caracteres alfanuméricos, RUC: 11 dígitos
    private void validarDocumento(String tipo, String numero) {
        boolean valido = switch (tipo) {
            case "DNI" -> numero.matches("\\d{8}");
            case "CE" -> numero.matches("[A-Z0-9]{9}");
            default -> numero.matches("\\d{11}");
        };
        if (!valido) {
            int longitud = "DNI".equals(tipo) ? 8 : "CE".equals(tipo) ? 9 : 11;
            throw new OperacionRechazadaException("Documento no válido: el " + tipo + " debe tener " + longitud
                    + " caracteres" + ("CE".equals(tipo) ? "." : " y solo números."));
        }
    }

    private void validarCondiciones(ClienteRequestDTO request) {
        boolean algunaNominal = Constantes.NOMINAL.equals(request.getTipoTasa())
                || Constantes.NOMINAL.equals(request.getTipoTasaMoratoria());
        if (algunaNominal) {
            int diasPeriodo = CalculadoraFinanciera.diasDelPeriodo(request.getPeriodoTasa());
            if (request.getPeriodoCapitalizacion() > diasPeriodo) {
                throw new OperacionRechazadaException("El periodo de capitalización debe estar entre 1 y "
                        + diasPeriodo + " días (periodo de la tasa).");
            }
        }
    }

    // Datos del cliente y su fila en condicion_credito (se crea en el primer registro)
    private void copiarCondiciones(ClienteRequestDTO request, Cliente cliente) {
        cliente.setNombreCliente(request.getNombreCliente().trim());
        CondicionCredito condicion = cliente.getCondicionCredito();
        if (condicion == null) {
            condicion = new CondicionCredito();
            condicion.setCliente(cliente);
            cliente.setCondicionCredito(condicion);
        }
        condicion.setMoneda(request.getMoneda());
        condicion.setLimiteCredito(request.getLimiteCredito());
        condicion.setTipoTasa(request.getTipoTasa());
        condicion.setPorcentajeTasa(request.getPorcentajeTasa());
        condicion.setTipoTasaMoratoria(request.getTipoTasaMoratoria());
        condicion.setPorcentajeTasaMoratoria(request.getPorcentajeTasaMoratoria());
        condicion.setPeriodoTasa(request.getPeriodoTasa());
        condicion.setPeriodoCapitalizacion(request.getPeriodoCapitalizacion());
        condicion.setDiaCorte(request.getDiaCorte());
        condicion.setDiaPagoPactado(request.getDiaPagoPactado());
        condicion.setMaximoMesesCredito(request.getMaximoMesesCredito());
    }

    private ClienteDTO aDTO(Cliente c) {
        CondicionCredito cc = c.getCondicionCredito();
        var utilizado = cuentaCorriente.creditoUtilizado(c, null);
        return ClienteDTO.builder()
                .idCliente(c.getIdCliente())
                .nombreCliente(c.getNombreCliente())
                .tipoDocumentoCliente(c.getTipoDocumentoCliente())
                .numeroDocumentoCliente(c.getNumeroDocumentoCliente())
                .estadoCliente(c.getEstadoCliente())
                .moneda(cc.getMoneda())
                .limiteCredito(cc.getLimiteCredito())
                .creditoUtilizado(utilizado)
                .creditoDisponible(CalculadoraFinanciera.redondear2(cc.getLimiteCredito().subtract(utilizado).max(java.math.BigDecimal.ZERO)))
                .tipoTasa(cc.getTipoTasa())
                .porcentajeTasa(cc.getPorcentajeTasa())
                .tipoTasaMoratoria(cc.getTipoTasaMoratoria())
                .porcentajeTasaMoratoria(cc.getPorcentajeTasaMoratoria())
                .periodoTasa(cc.getPeriodoTasa())
                .periodoCapitalizacion(cc.getPeriodoCapitalizacion())
                .diaCorte(cc.getDiaCorte())
                .diaPagoPactado(cc.getDiaPagoPactado())
                .maximoMesesCredito(cc.getMaximoMesesCredito())
                .tedCompensatoria(CalculadoraFinanciera.tasaEfectivaDiaria(cc.getTipoTasa(), cc.getPorcentajeTasa(),
                        cc.getPeriodoTasa(), cc.getPeriodoCapitalizacion()))
                .temCompensatoria(CalculadoraFinanciera.tasaEfectivaMensual(cc.getTipoTasa(), cc.getPorcentajeTasa(),
                        cc.getPeriodoTasa(), cc.getPeriodoCapitalizacion()))
                .tedMoratoria(CalculadoraFinanciera.tasaEfectivaDiaria(cc.getTipoTasaMoratoria(),
                        cc.getPorcentajeTasaMoratoria(), cc.getPeriodoTasa(), cc.getPeriodoCapitalizacion()))
                .fechaRegistro(c.getFechaRegistro())
                .build();
    }

    private Rol rol(String nombre) {
        return rolRepositorio.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombre + " en la tabla rol (import.sql)"));
    }
}
