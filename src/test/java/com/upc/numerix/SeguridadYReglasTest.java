package com.upc.numerix;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 Permisos por rol, aislamiento entre negocios y clientes, y reglas de negocio adicionales a los
 juegos de prueba. Usa la base de datos inicial (import.sql): Bodega Don Lucho con Rosa y
 Bazar La Esquina con Carlos.
 */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.hbm2ddl.import_files=/import.sql",
        "spring.datasource.url=jdbc:h2:mem:seguridad;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SeguridadYReglasTest {

    private static final String ROSA = "CLI-45871236";
    private static final String CARLOS = "CLI-70412589";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void cadaRolSoloAccedeASusEndpoints() throws Exception {
        String admin = login("admin", "Numerix2026");
        String bodega = login("donlucho", "Bodega2026");
        String rosa = login("45871236", "NX45871236");

        // Sin token o con token inválido: 401
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer token-falso")).andExpect(status().isUnauthorized());

        // AdminSistema: solo negocios y dashboard
        consultar(admin, "/api/admin/dashboard").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNegocios").value(2))
                .andExpect(jsonPath("$.totalNegociosActivos").value(2));
        consultar(admin, "/api/productos").andExpect(status().isForbidden());
        consultar(admin, "/api/clientes").andExpect(status().isForbidden());

        // AdminNegocio: no administra negocios
        consultar(bodega, "/api/negocios").andExpect(status().isForbidden());
        consultar(bodega, "/api/admin/dashboard").andExpect(status().isForbidden());

        // Cliente: solo consultas, nunca listados ni registros
        consultar(rosa, "/api/clientes").andExpect(status().isForbidden());
        consultar(rosa, "/api/productos").andExpect(status().isForbidden());
        consultar(rosa, "/api/negocios").andExpect(status().isForbidden());
        enviar(rosa, "/api/pagos", Map.of("idCuota", 1, "montoPagado", "1.00")).andExpect(status().isForbidden());
    }

    @Test
    void cadaNegocioSoloVeSusClientesYProductos() throws Exception {
        String bodega = login("donlucho", "Bodega2026");

        consultar(bodega, "/api/clientes").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idCliente").value(ROSA));
        consultar(bodega, "/api/productos").andExpect(jsonPath("$.length()").value(3));

        // Carlos y la licuadora son de Bazar La Esquina
        consultar(bodega, "/api/clientes/" + CARLOS).andExpect(status().isForbidden());
        consultar(bodega, "/api/clientes/" + CARLOS + "/reporte-deuda").andExpect(status().isForbidden());
        consultar(bodega, "/api/clientes/" + CARLOS + "/cronograma").andExpect(status().isForbidden());
        consultar(bodega, "/api/productos/PRD-00004").andExpect(status().isNotFound());
        enviar(bodega, "/api/compras", Map.of("idCliente", CARLOS, "idProducto", "PRD-00001")).andExpect(status().isForbidden());
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "idProducto", "PRD-00004")).andExpect(status().isNotFound());
    }

    @Test
    void cadaClienteSoloVeSusDatos() throws Exception {
        String rosa = login("45871236", "NX45871236");
        consultar(rosa, "/api/clientes/" + ROSA).andExpect(status().isOk());
        consultar(rosa, "/api/clientes/" + ROSA + "/reporte-deuda").andExpect(status().isOk());
        consultar(rosa, "/api/clientes/" + ROSA + "/compras").andExpect(status().isOk());
        consultar(rosa, "/api/clientes/" + CARLOS).andExpect(status().isForbidden());
        consultar(rosa, "/api/clientes/" + CARLOS + "/reporte-deuda").andExpect(status().isForbidden());
        consultar(rosa, "/api/clientes/" + CARLOS + "/pagos").andExpect(status().isForbidden());
    }

    @Test
    void negocioInactivoNoPuedeIngresar() throws Exception {
        String admin = login("admin", "Numerix2026");
        actualizar(admin, "/api/negocios/NEG-20609876541/estado", Map.of("estado", "Inactivo")).andExpect(status().isOk());

        for (String[] cuenta : new String[][]{{"laesquina", "Bazar2026"}, {"70412589", "NX70412589"}}) {
            mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("usuario", cuenta[0], "contrasena", cuenta[1]))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("La cuenta se encuentra inactiva."));
        }
        consultar(admin, "/api/admin/dashboard").andExpect(jsonPath("$.totalNegociosInactivos").value(1));
    }

    @Test
    void prepagoYSobrepagoDeUnaCompraAUnSoloPago() throws Exception {
        String bodega = login("donlucho", "Bodega2026");
        // Compra C-003 del juego 1: vence el 30/10 por S/ 61.46
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "idProducto", "PRD-00003",
                "cantidadProducto", 15, "fechaCompra", "2026-09-27"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoTotalPagar").value(61.46));
        long idCuota = mapper.readTree(consultar(bodega, "/api/clientes/" + ROSA + "/obligaciones-pendientes")
                .andReturn().getResponse().getContentAsString()).get(0).get("idCuota").asLong();

        // Prepago 10 días antes: 61.46 / (1 + TED)^10 = 61.01; el cliente se ahorra el interés no usado
        consultar(bodega, "/api/pagos/simular?idCuota=" + idCuota + "&fechaPagoReal=2026-10-20")
                .andExpect(jsonPath("$.diasAnticipacion").value(10))
                .andExpect(jsonPath("$.montoTotalObligacion").value(61.01))
                .andExpect(jsonPath("$.interesCompensatorioDeuda").value(1.01))
                .andExpect(jsonPath("$.capitalPendienteDeuda").value(60.00));

        // No se acepta pagar de más ni de menos
        enviar(bodega, "/api/pagos", Map.of("idCuota", idCuota, "fechaPagoReal", "2026-10-20", "montoPagado", "61.46"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El monto pagado excede el monto a pagar: S/ 61.01."));
        enviar(bodega, "/api/pagos", Map.of("idCuota", idCuota, "fechaPagoReal", "2026-10-20", "montoPagado", "61.00"))
                .andExpect(status().is(422));
        enviar(bodega, "/api/pagos", Map.of("idCuota", idCuota, "fechaPagoReal", "2026-10-20", "montoPagado", "61.01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoImputadoInteres").value(1.01))
                .andExpect(jsonPath("$.montoImputadoCapital").value(60.00))
                .andExpect(jsonPath("$.estadoDeuda").value("Pagado"))
                .andExpect(jsonPath("$.creditoDisponible").value(500.00));

        // Una cuota pagada no se vuelve a pagar
        enviar(bodega, "/api/pagos", Map.of("idCuota", idCuota, "fechaPagoReal", "2026-10-21", "montoPagado", "61.01"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("La cuota ya se encuentra pagada."));
    }

    @Test
    void modalidadDeVentaYProductoInactivo() throws Exception {
        String bodega = login("donlucho", "Bodega2026");
        actualizar(bodega, "/api/productos/PRD-00001", Map.of("descripcionProducto", "Arroz Costeño 5 kg",
                "precioContado", "28.00", "precioCredito", "30.00", "modalidadVentaProducto", "Contado"))
                .andExpect(status().isOk());
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "idProducto", "PRD-00001",
                "tipoPago", "Cuotas", "numeroCuotas", 2, "fechaCompra", "2026-09-10"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El producto solo se vende a un solo pago."));

        actualizar(bodega, "/api/productos/PRD-00002/estado", Map.of("estado", "Inactivo")).andExpect(status().isOk());
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "idProducto", "PRD-00002", "fechaCompra", "2026-09-10"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El producto se encuentra inactivo y no puede venderse."));

        // Precio crédito menor al contado: no se registra el producto
        enviar(bodega, "/api/productos", Map.of("descripcionProducto", "Azúcar", "precioContado", "5.00",
                "precioCredito", "4.50", "modalidadVentaProducto", "Ambas"))
                .andExpect(status().is(422));
    }

    // ---------- Utilidades ----------

    private String login(String usuario, String contrasena) throws Exception {
        String respuesta = mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("usuario", usuario, "contrasena", contrasena))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("jwt").asText();
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
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }
}
