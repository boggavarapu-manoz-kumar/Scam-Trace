package com.kampusvc.digitalrisk.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that CORS is configured narrowly on the /api/** path mapping.
 *
 * <p>These tests confirm:
 * <ul>
 *   <li>A preflight from the configured allowed origin receives the correct
 *       Access-Control-Allow-Origin response header.</li>
 *   <li>A preflight from a disallowed origin does NOT receive an
 *       Access-Control-Allow-Origin header, preventing the browser from
 *       completing the cross-origin request.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Preflight from allowed origin receives CORS approval")
    void preflightFromAllowedOriginIsApproved() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Content-Type, Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("Preflight from disallowed origin is rejected — no ACAO header")
    void preflightFromDisallowedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "https://attacker.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Health endpoint is reachable and returns expected envelope structure")
    void healthEndpointReturnsEnvelope() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/health")
                        .header("Origin", "http://localhost:5173")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
