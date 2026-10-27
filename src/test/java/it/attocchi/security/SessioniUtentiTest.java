package it.attocchi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Chiusura delle sessioni di un utente (cambio di username) sul filtro reale di Spring Security. */
@SpringBootTest
class SessioniUtentiTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private SessioniUtenti sessioniUtenti;

    private MockMvc mvc;

    @BeforeEach
    void prepara() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void sessioneChiusa_siTornaAllAccesso() throws Exception {
        MockHttpSession sessione = (MockHttpSession) mvc.perform(post("/login")
                .param("username", "insegnante2").param("password", "insegnante2").with(csrf()))
            .andReturn().getRequest().getSession();
        // Ancora dentro: l'health check con i dettagli non serve, basta che non rimandi all'accesso
        String prima = mvc.perform(get("/actuator/health").session(sessione)).andReturn().getResponse().getRedirectedUrl();
        assertNotEquals("/login", prima);

        assertEquals(0, sessioniUtenti.chiudi(List.of("nessuno")));
        assertEquals(1, sessioniUtenti.chiudi(List.of("insegnante2")));
        assertEquals("/login", mvc.perform(get("/actuator/health").session(sessione)).andReturn().getResponse()
            .getRedirectedUrl());
    }
}
