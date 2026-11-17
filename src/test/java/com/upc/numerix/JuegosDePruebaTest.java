package com.upc.numerix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 Ejecuta los juegos de prueba 1 a 4 del informe (apartado 6.4) contra la API completa:
 seguridad JWT, controladores, servicios y base de datos H2 en memoria.
 Los importes esperados son los de las tablas del informe.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JuegosDePruebaTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    // ---------- Juego 1 y casos 3.1, 3.2 y 3.5 ----------

    @Test
    void juego1_comprasAUnSoloPagoConMora() throws Exception {
        String admin = login("admin", "Numerix2026");
        registrarNegocio(admin, "Bodega Don Lucho", "Bodega", "20111111111", "bodega1", "Bodega2026");
        String negocio = login("bodega1", "Bodega2026");

        enviar(negocio, "/api/clientes", Map.ofEntries(
                Map.entry("nombreCliente", "Rosa Quispe Huamán"), Map.entry("tipoDocumentoCliente", "DNI"),
                Map.entry("numeroDocumentoCliente", "45871236"), Map.entry("moneda", "PEN"),
                Map.entry("limiteCredito", 500), Map.entry("tipoTasa", "Efectiva"), Map.entry("porcentajeTasa", 30),
                Map.entry("tipoTasaMoratoria", "Efectiva"), Map.entry("porcentajeTasaMoratoria", 15),
                Map.entry("periodoTasa", "Anual"), Map.entry("diaCorte", 25), Map.entry("diaPagoPactado", 30),
                Map.entry("maximoMesesCredito", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("45871236"))
                .andExpect(jsonPath("$.contrasena").value("NX45871236"))
                .andExpect(jsonPath("$.cliente.tedCompensatoria").value(0.0007291))
                .andExpect(jsonPath("$.cliente.tedMoratoria").value(0.0003883));
        String rosa = "CLI-45871236";

        String arroz = producto(negocio, "Arroz Costeño 5 kg", "28.00", "30.00");
        String aceite = producto(negocio, "Aceite Primor 1 L", "9.50", "10.00");
        String leche = producto(negocio, "Leche Gloria 400 g", "3.80", "4.00");

        // Compras C-001, C-002 y C-003 (C-003 es posterior al corte del 25/09: se paga el 30/10)
        String c001 = compra(negocio, rosa, arroz, 4, "Un solo pago", null, "2026-09-10")
                .andExpect(jsonPath("$.montoCompra").value(120.00))
                .andExpect(jsonPath("$.fechaPagoPactada").value("2026-09-30"))
                .andExpect(jsonPath("$.diasOperacion").value(20))
                .andExpect(jsonPath("$.interesCompensatorioDeuda").value(1.76))
                .andExpect(jsonPath("$.montoTotalPagar").value(121.76))
                .andReturn().getResponse().getContentAsString();
        compra(negocio, rosa, aceite, 8, "Un solo pago", null, "2026-09-22")
                .andExpect(jsonPath("$.interesCompensatorioDeuda").value(0.47));
        compra(negocio, rosa, leche, 15, "Un solo pago", null, "2026-09-27")
                .andExpect(jsonPath("$.fechaPagoPactada").value("2026-10-30"))
                .andExpect(jsonPath("$.diasOperacion").value(33))
                .andExpect(jsonPath("$.interesCompensatorioDeuda").value(1.46))
                .andExpect(jsonPath("$.creditoDisponibleDespues").value(240.00));

        // Reporte de deuda en la fecha de corte: C-003 no aparece
        consultar(negocio, "/api/clientes/" + rosa + "/reporte-deuda?fecha=2026-09-25")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximaFechaPago").value("2026-09-30"))
                .andExpect(jsonPath("$.deudas.length()").value(2))
                .andExpect(jsonPath("$.deudas[0].interesCompensatorioDeuda").value(1.76))
                .andExpect(jsonPath("$.deudas[1].interesCompensatorioDeuda").value(0.47))
                .andExpect(jsonPath("$.capitalTotal").value(200.00))
                .andExpect(jsonPath("$.interesCompensatorioTotal").value(2.23))
                .andExpect(jsonPath("$.interesMoratorioTotal").value(0.00))
                .andExpect(jsonPath("$.montoTotalPagar").value(202.23));

        // 3.1: compra que excede el crédito disponible
        compra(negocio, rosa, arroz, 10, "Un solo pago", null, "2026-09-28", 422)
                .andExpect(jsonPath("$.estadoOperacion").value("Rechazada"))
                .andExpect(jsonPath("$.mensaje").value("El monto de la compra excede el crédito disponible (S/ 240.00)."));

        // 3.5: compra igual al crédito disponible (caso límite permitido)
        compra(negocio, rosa, arroz, 8, "Un solo pago", null, "2026-09-28")
                .andExpect(jsonPath("$.estadoOperacion").value("Procesada"))
                .andExpect(jsonPath("$.mensaje").value("Compra registrada. Crédito disponible: S/ 0.00."));

        JsonNode pendientes = json(consultar(negocio, "/api/clientes/" + rosa + "/obligaciones-pendientes"));
        long cuotaC001 = pendientes.get(0).get("idCuota").asLong();
        long cuotaC002 = pendientes.get(1).get("idCuota").asLong();

        // 3.2: pago parcial rechazado
        pago(negocio, cuotaC001, "2026-10-06", "100.00", 422)
                .andExpect(jsonPath("$.mensaje").value("No se permiten pagos parciales. Monto a pagar: S/ 122.04."));

        // Pagos del 06/10 con 6 días de mora; prelación mora -> interés -> capital
        consultar(negocio, "/api/pagos/simular?idCuota=" + cuotaC001 + "&fechaPagoReal=2026-10-06")
                .andExpect(jsonPath("$.diasMora").value(6))
                .andExpect(jsonPath("$.interesMoratorioObligacion").value(0.28))
                .andExpect(jsonPath("$.montoTotalObligacion").value(122.04));
        pago(negocio, cuotaC001, "2026-10-06", "122.04", 201)
                .andExpect(jsonPath("$.estadoOperacion").value("Procesada"))
                .andExpect(jsonPath("$.montoImputadoMora").value(0.28))
                .andExpect(jsonPath("$.montoImputadoInteres").value(1.76))
                .andExpect(jsonPath("$.montoImputadoCapital").value(120.00))
                .andExpect(jsonPath("$.estadoDeuda").value("Pagado"));
        pago(negocio, cuotaC002, "2026-10-06", "80.66", 201)
                .andExpect(jsonPath("$.montoImputadoMora").value(0.19))
                .andExpect(jsonPath("$.montoImputadoInteres").value(0.47))
                .andExpect(jsonPath("$.montoImputadoCapital").value(80.00))
                // 500 - 60 (C-003) - 240 (compra del caso 3.5)
                .andExpect(jsonPath("$.creditoDisponible").value(200.00));

        // Después del pago, la próxima fecha de pago es el 30/10 con C-003 por S/ 61.46
        consultar(negocio, "/api/clientes/" + rosa + "/reporte-deuda?fecha=2026-10-06")
                .andExpect(jsonPath("$.proximaFechaPago").value("2026-10-30"))
                .andExpect(jsonPath("$.deudas[0].montoTotalDeuda").value(61.46));

        consultar(negocio, "/api/clientes/" + rosa + "/pagos?fechaInicioConsulta=2026-10-01&fechaFinConsulta=2026-10-31")
                .andExpect(jsonPath("$.length()").value(2));

        // El cronograma es solo para compras en cuotas: Rosa solo tiene compras a un solo pago
        consultar(negocio, "/api/clientes/" + rosa + "/cronograma")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronogramas.length()").value(0))
                .andExpect(jsonPath("$.proximaFechaPago").doesNotExist());
        String idC001 = mapper.readTree(c001).get("idCompra").asText();
        consultar(negocio, "/api/compras/" + idC001 + "/cronograma")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("La compra " + idC001 + " es a un solo pago: no tiene cronograma de pagos."));

        // Seguridad: la clienta solo ve su información y no puede registrar compras
        String clienta = login("45871236", "NX45871236");
        consultar(clienta, "/api/clientes/" + rosa + "/reporte-deuda").andExpect(status().isOk());
        compra(clienta, rosa, arroz, 1, "Un solo pago", null, "2026-10-10", 403);
        consultar(admin, "/api/clientes/" + rosa).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        assert c001.contains("CMP-");
    }

    // ---------- Juego 2, juego 4 y casos 3.3 y 3.4 ----------

    @Test
    void juego2y4_compraEnCuotasConGraciaTotalMoraYPrepago() throws Exception {
        String admin = login("admin", "Numerix2026");
        registrarNegocio(admin, "Bazar La Esquina", "Bazar", "20222222222", "bazar1", "Bazar2026");
        String negocio = login("bazar1", "Bazar2026");

        enviar(negocio, "/api/clientes", Map.ofEntries(
                Map.entry("nombreCliente", "Carlos Mendoza Ríos"), Map.entry("tipoDocumentoCliente", "DNI"),
                Map.entry("numeroDocumentoCliente", "70412589"), Map.entry("moneda", "PEN"),
                Map.entry("limiteCredito", 1000), Map.entry("tipoTasa", "Nominal"), Map.entry("porcentajeTasa", 24),
                Map.entry("tipoTasaMoratoria", "Nominal"), Map.entry("porcentajeTasaMoratoria", 12),
                Map.entry("periodoTasa", "Anual"), Map.entry("periodoCapitalizacion", 30),
                Map.entry("diaCorte", 20), Map.entry("diaPagoPactado", 26), Map.entry("maximoMesesCredito", 6)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cliente.temCompensatoria").value(0.02))
                .andExpect(jsonPath("$.cliente.tedCompensatoria").value(0.0006603))
                .andExpect(jsonPath("$.cliente.tedMoratoria").value(0.0003317));
        String carlos = "CLI-70412589";
        String licuadora = producto(negocio, "Licuadora Oster 1.5 L", "420.00", "450.00");
        String tostadora = producto(negocio, "Tostadora Imaco", "45.00", "50.00");

        // 3.3: más cuotas que el máximo permitido
        compra(negocio, carlos, licuadora, 1, "Cuotas", 8, "2026-09-15", 422)
                .andExpect(jsonPath("$.mensaje").value("El número de cuotas supera el máximo permitido (6)."));

        String idCompra = json(compra(negocio, carlos, licuadora, 1, "Cuotas", 3, "2026-09-15")
                .andExpect(jsonPath("$.diasGracia").value(11))
                .andExpect(jsonPath("$.factorCapitalizacion").value(1.0072873))
                .andExpect(jsonPath("$.capitalActualizado").value(453.28))
                .andExpect(jsonPath("$.montoCuota").value(157.18))
                .andExpect(jsonPath("$.fechaPagoPactada").value("2026-10-26"))
                .andExpect(jsonPath("$.creditoDisponibleDespues").value(550.00))).get("idCompra").asText();

        JsonNode cronograma = json(consultar(negocio, "/api/compras/" + idCompra + "/cronograma")
                .andExpect(jsonPath("$.cuotas[0].fechaVencimientoPeriodo").value("2026-10-26"))
                .andExpect(jsonPath("$.cuotas[0].interesPeriodo").value(9.07))
                .andExpect(jsonPath("$.cuotas[0].amortizacionPeriodo").value(148.11))
                .andExpect(jsonPath("$.cuotas[0].saldoFinalPeriodo").value(305.17))
                .andExpect(jsonPath("$.cuotas[1].fechaVencimientoPeriodo").value("2026-11-26"))
                .andExpect(jsonPath("$.cuotas[1].interesPeriodo").value(6.10))
                .andExpect(jsonPath("$.cuotas[1].amortizacionPeriodo").value(151.08))
                .andExpect(jsonPath("$.cuotas[1].cuotaPeriodo").value(157.18))
                .andExpect(jsonPath("$.cuotas[2].fechaVencimientoPeriodo").value("2026-12-26"))
                .andExpect(jsonPath("$.cuotas[2].interesPeriodo").value(3.08))
                .andExpect(jsonPath("$.cuotas[2].cuotaPeriodo").value(157.17))
                .andExpect(jsonPath("$.cuotas[2].saldoFinalPeriodo").value(0.00))
                .andExpect(jsonPath("$.totalInteres").value(18.25))
                .andExpect(jsonPath("$.totalAmortizacion").value(453.28))
                .andExpect(jsonPath("$.totalCuotas").value(471.53)));
        long cuota1 = cronograma.get("cuotas").get(0).get("idCuota").asLong();
        long cuota2 = cronograma.get("cuotas").get(1).get("idCuota").asLong();
        long cuota3 = cronograma.get("cuotas").get(2).get("idCuota").asLong();

        // Pantalla Cronograma: se elige el cliente y se ven sus compras en cuotas (no la tostadora a un solo pago)
        compra(negocio, carlos, tostadora, 1, "Un solo pago", null, "2026-09-16");
        consultar(negocio, "/api/clientes/" + carlos + "/cronograma")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronogramas.length()").value(1))
                .andExpect(jsonPath("$.cronogramas[0].idCompra").value(idCompra))
                .andExpect(jsonPath("$.cronogramas[0].cuotas.length()").value(3))
                .andExpect(jsonPath("$.proximaFechaPago").value("2026-10-26"));

        // Cuota 1 en la fecha pactada: sin mora
        pago(negocio, cuota1, "2026-10-26", "157.18", 201)
                .andExpect(jsonPath("$.montoImputadoMora").value(0.00));
        consultar(negocio, "/api/clientes/" + carlos + "/cronograma")
                .andExpect(jsonPath("$.proximaFechaPago").value("2026-11-26"))
                .andExpect(jsonPath("$.cronogramas[0].cuotas[0].estadoCuota").value("Pagado"));

        // No se puede saltar una cuota
        pago(negocio, cuota3, "2026-11-30", "157.17", 422)
                .andExpect(jsonPath("$.mensaje").value("Debe pagar primero la cuota N.º 2."));

        // Cuota 2 con 4 días de atraso
        consultar(negocio, "/api/pagos/simular?idCuota=" + cuota2 + "&fechaPagoReal=2026-11-30")
                .andExpect(jsonPath("$.diasMora").value(4))
                .andExpect(jsonPath("$.interesMoratorioObligacion").value(0.21))
                .andExpect(jsonPath("$.montoTotalObligacion").value(157.39));
        pago(negocio, cuota2, "2026-11-30", "157.39", 201)
                .andExpect(jsonPath("$.montoImputadoMora").value(0.21))
                .andExpect(jsonPath("$.montoImputadoInteres").value(6.10))
                .andExpect(jsonPath("$.montoImputadoCapital").value(151.08));

        // Juego 4: prepago de la cuota 3 el 11/12 (15 días antes)
        consultar(negocio, "/api/pagos/simular?idCuota=" + cuota3 + "&fechaPagoReal=2026-12-11")
                .andExpect(jsonPath("$.diasAnticipacion").value(15))
                .andExpect(jsonPath("$.montoTotalObligacion").value(155.62))
                .andExpect(jsonPath("$.interesCompensatorioDeuda").value(1.53))
                .andExpect(jsonPath("$.capitalPendienteDeuda").value(154.09));
        pago(negocio, cuota3, "2026-12-11", "155.62", 201)
                .andExpect(jsonPath("$.montoImputadoMora").value(0.00))
                .andExpect(jsonPath("$.montoImputadoInteres").value(1.53))
                .andExpect(jsonPath("$.montoImputadoCapital").value(154.09))
                .andExpect(jsonPath("$.estadoDeuda").value("Pagado"))
                // 1000 - 50 de la tostadora a un solo pago, que sigue pendiente
                .andExpect(jsonPath("$.creditoDisponible").value(950.00));

        consultar(negocio, "/api/compras/" + idCompra + "/cronograma")
                .andExpect(jsonPath("$.estadoDeuda").value("Pagado"))
                .andExpect(jsonPath("$.cuotasPagadas").value(3));

        // 3.4: cliente inactivo
        actualizar(negocio, "/api/clientes/" + carlos + "/estado", Map.of("estado", "Inactivo")).andExpect(status().isOk());
        compra(negocio, carlos, tostadora, 1, "Un solo pago", null, "2026-12-15", 422)
                .andExpect(jsonPath("$.mensaje").value("El cliente se encuentra inactivo y no puede registrar nuevas compras."));
    }

    @Test
    void validacionesDeRegistro() throws Exception {
        String admin = login("admin", "Numerix2026");
        enviar(admin, "/api/negocios", Map.of("nombreNegocio", "X", "rucNegocio", "2012345", "tipoNegocio", "Bodega",
                "nombreAdministradorNegocio", "Ana", "correoElectronico", "ana@x.pe", "usuario", "x1", "contrasena", "12345678"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.rucNegocio").exists());
        mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin\",\"contrasena\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        consultar(admin, "/api/admin/dashboard").andExpect(status().isOk());
    }

    // Valores por defecto de la tabla de datos de entrada cuando el campo no se envía
    @Test
    void valoresPorDefectoDeLaTablaDeDatos() throws Exception {
        String admin = login("admin", "Numerix2026");
        registrarNegocio(admin, "Frutería Doña Ana", "Frutería", "10333333333", "fruteria1", "Fruta2026");
        String negocio = login("fruteria1", "Fruta2026");

        enviar(negocio, "/api/clientes", Map.of("nombreCliente", "Ana Pérez", "numeroDocumentoCliente", "12345678",
                "limiteCredito", 300, "porcentajeTasa", 2, "porcentajeTasaMoratoria", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cliente.tipoDocumentoCliente").value("DNI"))
                .andExpect(jsonPath("$.cliente.moneda").value("PEN"))
                .andExpect(jsonPath("$.cliente.tipoTasa").value("Efectiva"))
                .andExpect(jsonPath("$.cliente.tipoTasaMoratoria").value("Efectiva"))
                .andExpect(jsonPath("$.cliente.periodoTasa").value("Mensual"))
                .andExpect(jsonPath("$.cliente.periodoCapitalizacion").value(30))
                .andExpect(jsonPath("$.cliente.diaCorte").value(25))
                .andExpect(jsonPath("$.cliente.diaPagoPactado").value(30))
                .andExpect(jsonPath("$.cliente.maximoMesesCredito").value(1))
                .andExpect(jsonPath("$.cliente.estadoCliente").value("Activo"));

        JsonNode producto = json(enviar(negocio, "/api/productos", Map.of("descripcionProducto", "Manzana",
                "precioContado", "1.00", "precioCredito", "1.20", "modalidadVentaProducto", "Ambas"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidadMedida").value("Unidad"))
                .andExpect(jsonPath("$.estadoProducto").value("Activo")));

        enviar(negocio, "/api/compras", Map.of("idCliente", "CLI-12345678",
                "idProducto", producto.get("idProducto").asText(), "fechaCompra", "2026-11-02"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoPago").value("Un solo pago"))
                .andExpect(jsonPath("$.cantidadProducto").value(1))
                .andExpect(jsonPath("$.numeroCuotas").value(1))
                .andExpect(jsonPath("$.fechaPagoPactada").value("2026-11-30"))
                .andExpect(jsonPath("$.montoCuota").doesNotExist());

        // recordarContrasena alarga la sesión y se devuelve autenticacionValida
        mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"12345678\",\"contrasena\":\"NX12345678\",\"recordarContrasena\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autenticacionValida").value(true))
                .andExpect(jsonPath("$.recordarContrasena").value(true));
    }

    // ---------- Utilidades ----------

    private String login(String usuario, String contrasena) throws Exception {
        String respuesta = mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("usuario", usuario, "contrasena", contrasena))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("jwt").asText();
    }

    private void registrarNegocio(String token, String nombre, String tipo, String ruc, String usuario,
                                  String contrasena) throws Exception {
        enviar(token, "/api/negocios", Map.of("nombreNegocio", nombre, "rucNegocio", ruc, "tipoNegocio", tipo,
                "nombreAdministradorNegocio", "Administrador " + nombre, "correoElectronico", usuario + "@numerix.pe",
                "usuario", usuario, "contrasena", contrasena))
                .andExpect(status().isCreated());
    }

    private String producto(String token, String descripcion, String contado, String credito) throws Exception {
        return json(enviar(token, "/api/productos", Map.of("descripcionProducto", descripcion, "unidadMedida", "Unidad",
                "precioContado", contado, "precioCredito", credito, "modalidadVentaProducto", "Ambas"))
                .andExpect(status().isCreated())).get("idProducto").asText();
    }

    private ResultActions compra(String token, String cliente, String producto, int cantidad, String tipoPago,
                                 Integer cuotas, String fecha) throws Exception {
        return compra(token, cliente, producto, cantidad, tipoPago, cuotas, fecha, 201);
    }

    private ResultActions compra(String token, String cliente, String producto, int cantidad, String tipoPago,
                                 Integer cuotas, String fecha, int estadoHttp) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("idCliente", cliente, "idProducto", producto,
                "cantidadProducto", cantidad, "tipoPago", tipoPago, "fechaCompra", fecha));
        if (cuotas != null) {
            body.put("numeroCuotas", cuotas);
        }
        return enviar(token, "/api/compras", body).andExpect(status().is(estadoHttp));
    }

    private ResultActions pago(String token, long idCuota, String fecha, String monto, int estadoHttp) throws Exception {
        return enviar(token, "/api/pagos", Map.of("idCuota", idCuota, "fechaPagoReal", fecha, "montoPagado", monto))
                .andExpect(status().is(estadoHttp));
    }

    private ResultActions enviar(String token, String url, Object body) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private ResultActions actualizar(String token, String url, Object body) throws Exception {
        return mvc.perform(put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private ResultActions consultar(String token, String url) throws Exception {
        return mvc.perform(get(url)
                .header("Authorization", "Bearer " + token));
    }

    private JsonNode json(ResultActions resultado) throws Exception {
        return mapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }
}
