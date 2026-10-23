package it.attocchi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Health check per il monitoraggio sul filtro di sicurezza reale (dati di DataInitializer). */
@SpringBootTest
class HealthCheckTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void prepara() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void senzaLogin_soloLoStato() throws Exception {
        MockHttpServletResponse risposta = mvc.perform(get("/actuator/health")).andReturn().getResponse();
        assertNull(risposta.getRedirectedUrl());
        assertEquals(200, risposta.getStatus());
        assertEquals("{\"status\":\"UP\"}", risposta.getContentAsString());
    }

    @Test
    @WithUserDetails("admin")
    void admin_vedeIDettagliSenzaIlServerEmail() throws Exception {
        String corpo = mvc.perform(get("/actuator/health")).andReturn().getResponse().getContentAsString();
        assertTrue(corpo.contains("\"db\""), corpo);
        assertFalse(corpo.contains("\"mail\""), corpo);
    }

    @Test
    @WithUserDetails("segreteria")
    void altriUtenti_soloLoStato() throws Exception {
        assertEquals("{\"status\":\"UP\"}", mvc.perform(get("/actuator/health")).andReturn().getResponse().getContentAsString());
    }

    @Test
    void altriEndpointNonEsposti() throws Exception {
        for (String percorso : new String[] {"/actuator/env", "/actuator/beans", "/actuator/info"}) {
            MockHttpServletResponse risposta = mvc.perform(get(percorso)).andReturn().getResponse();
            assertNotEquals(200, risposta.getStatus(), percorso);
        }
    }
}
