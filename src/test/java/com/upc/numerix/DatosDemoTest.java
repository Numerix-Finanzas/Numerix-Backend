package com.upc.numerix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifica que la base de datos inicial (import.sql) se carga y que sus credenciales funcionan
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.hbm2ddl.import_files=/import.sql", "spring.datasource.url=jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class DatosDemoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void cargaNegociosClientesYProductosDeDemo() throws Exception {
        String respuesta = mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"donlucho\",\"contrasena\":\"Bodega2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolUsuario").value("AdminNegocio"))
                .andExpect(jsonPath("$.nombreNegocio").value("Bodega Don Lucho"))
                .andReturn().getResponse().getContentAsString();
        String jwt = respuesta.replaceAll(".*\"jwt\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/productos").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer " + jwt))
                .andExpect(jsonPath("$[0].nombreCliente").value("Rosa Quispe Huamán"))
                .andExpect(jsonPath("$[0].creditoDisponible").value(500.00));

        mvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"70412589\",\"contrasena\":\"NX70412589\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCliente").value("CLI-70412589"));
    }
}
