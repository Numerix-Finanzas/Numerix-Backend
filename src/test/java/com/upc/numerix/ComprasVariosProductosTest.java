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

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 Compras con uno o varios productos distintos en una sola compra. Usa la base de datos inicial
 (import.sql): Bodega Don Lucho con Rosa y Bazar La Esquina con Carlos.
 */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:varios;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ComprasVariosProductosTest {

    private static final String ROSA = "CLI-45871236";
    private static final String CARLOS = "CLI-70412589";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void compraDeVariosProductosAUnSoloPago() throws Exception {
        String bodega = login("donlucho", "Bodega2026");
        // 2 arroces (2 × 30.00) + 6 aceites (6 × 10.00) = 120.00: mismo total que C-001 del juego 1
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10", "tipoPago", "Un solo pago",
                "detalles", List.of(linea("PRD-00001", 2), linea("PRD-00002", 6))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detalles.length()").value(2))
                .andExpect(jsonPath("$.detalles[0].descripcionProducto").value("Arroz Costeño 5 kg"))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(60.00))
                .andExpect(jsonPath("$.detalles[1].precioCredito").value(10.00))
                .andExpect(jsonPath("$.detalles[1].subtotal").value(60.00))
                .andExpect(jsonPath("$.descripcionProducto").value("Arroz Costeño 5 kg, Aceite Primor 1 L"))
                .andExpect(jsonPath("$.cantidadProducto").value(8))
                .andExpect(jsonPath("$.montoCompra").value(120.00))
                // El interés se calcula sobre el total de la compra, igual que en C-001
                .andExpect(jsonPath("$.interesCompensatorioDeuda").value(1.76))
                .andExpect(jsonPath("$.montoTotalPagar").value(121.76))
                .andExpect(jsonPath("$.creditoDisponibleDespues").value(380.00));

        // Una sola obligación y una sola fila de movimientos por la compra completa
        consultar(bodega, "/api/clientes/" + ROSA + "/obligaciones-pendientes")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deudaVencida").value(121.76))
                .andExpect(jsonPath("$[0].descripcionProducto").value("Arroz Costeño 5 kg, Aceite Primor 1 L"));
        consultar(bodega, "/api/clientes/" + ROSA + "/compras")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].detalles.length()").value(2))
                .andExpect(jsonPath("$[0].cantidadProducto").value(8));
    }

    @Test
    void compraDeVariosProductosEnCuotas() throws Exception {
        String bazar = login("laesquina", "Bazar2026");
        // 100 papeles higiénicos (450.00) + 10 detergentes (50.00) = 500.00 en 3 cuotas con las condiciones del juego 2
        String idCompra = mapper.readTree(enviar(bazar, "/api/compras", Map.of("idCliente", CARLOS,
                        "fechaCompra", "2026-09-15", "tipoPago", "Cuotas", "numeroCuotas", 3,
                        "detalles", List.of(linea("PRD-00004", 100), linea("PRD-00005", 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoCompra").value(500.00))
                .andExpect(jsonPath("$.diasGracia").value(11))
                .andExpect(jsonPath("$.capitalActualizado").value(503.64))
                .andExpect(jsonPath("$.montoCuota").value(174.64))
                .andExpect(jsonPath("$.montoTotalPagar").value(523.91))
                .andExpect(jsonPath("$.creditoDisponibleDespues").value(500.00))
                .andReturn().getResponse().getContentAsString()).get("idCompra").asText();

        consultar(bazar, "/api/compras/" + idCompra + "/cronograma")
                .andExpect(jsonPath("$.descripcionProducto").value("Papel higiénico Elite 4 rollos, Detergente Bolívar 500 g"))
                .andExpect(jsonPath("$.cuotas[0].interesPeriodo").value(10.07))
                .andExpect(jsonPath("$.cuotas[2].cuotaPeriodo").value(174.63))
                .andExpect(jsonPath("$.cuotas[2].saldoFinalPeriodo").value(0.00))
                .andExpect(jsonPath("$.totalAmortizacion").value(503.64));
    }

    @Test
    void rechazosDeUnaCompraConVariosProductos() throws Exception {
        String bodega = login("donlucho", "Bodega2026");

        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("Agregue al menos un producto a la compra."));
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10",
                "detalles", List.of(linea("PRD-00001", 1), linea("PRD-00001", 2))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El producto Arroz Costeño 5 kg está repetido en la compra: indique la cantidad total en una sola línea."));
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10",
                "detalles", List.of(linea("PRD-00001", 0))))
                .andExpect(status().isBadRequest());
        // Un producto de otro negocio dentro de la lista
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10",
                "detalles", List.of(linea("PRD-00001", 1), linea("PRD-00004", 1))))
                .andExpect(status().isNotFound());
        // El límite de crédito se compara con el total: 10 × 30 + 21 × 10 = 510 > 500
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10",
                "detalles", List.of(linea("PRD-00001", 10), linea("PRD-00002", 21))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El monto de la compra excede el crédito disponible (S/ 500.00)."));
        // Un producto que no admite el tipo de pago rechaza toda la compra
        actualizar(bodega, "/api/productos/PRD-00003", Map.of("descripcionProducto", "Leche Gloria 400 g",
                "precioContado", "3.80", "precioCredito", "4.00", "modalidadVentaProducto", "Contado"))
                .andExpect(status().isOk());
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10", "tipoPago", "Cuotas",
                "numeroCuotas", 2, "detalles", List.of(linea("PRD-00001", 1), linea("PRD-00003", 5))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El producto Leche Gloria 400 g solo se vende a un solo pago."));
        // Un producto inactivo rechaza toda la compra
        actualizar(bodega, "/api/productos/PRD-00002/estado", Map.of("estado", "Inactivo")).andExpect(status().isOk());
        enviar(bodega, "/api/compras", Map.of("idCliente", ROSA, "fechaCompra", "2026-09-10",
                "detalles", List.of(linea("PRD-00001", 1), linea("PRD-00002", 1))))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El producto Aceite Primor 1 L se encuentra inactivo y no puede venderse."));

        // Ningún rechazo registró nada
        consultar(bodega, "/api/clientes/" + ROSA + "/compras").andExpect(jsonPath("$.length()").value(0));
        consultar(bodega, "/api/clientes/" + ROSA).andExpect(jsonPath("$.creditoDisponible").value(500.00));
    }

    // ---------- Utilidades ----------

    private Map<String, Object> linea(String idProducto, int cantidad) {
        return Map.of("idProducto", idProducto, "cantidadProducto", cantidad);
    }

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
