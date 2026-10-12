package com.kampusvc.digitalrisk.common.exception;

import com.kampusvc.digitalrisk.DigitalRiskPlatformApplication;
import com.kampusvc.digitalrisk.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DigitalRiskPlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerTest.TestExceptionController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api/v1/test-exceptions")
    static class TestExceptionController {

        public record TestPayload(@NotBlank(message = "Title must not be blank") String title) {}

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Requested brand asset not found");
        }

        @GetMapping("/unhandled")
        public void throwUnhandled() {
            throw new RuntimeException("Simulated unexpected failure");
        }

        @PostMapping("/validate")
        public ApiResponse<String> validatePayload(@Valid @RequestBody TestPayload payload) {
            return ApiResponse.success("Accepted", payload.title());
        }
    }

    @Test
    void shouldHandleResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Requested brand asset not found")))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", is("Requested brand asset not found")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldHandleValidationErrorsWithoutLeakingStackTraces() throws Exception {
        mockMvc.perform(post("/api/v1/test-exceptions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details.title", is("Title must not be blank")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void shouldHandleMalformedJsonPayload() throws Exception {
        mockMvc.perform(post("/api/v1/test-exceptions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("MALFORMED_REQUEST")));
    }

    @Test
    void shouldHandleMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/test-exceptions/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("METHOD_NOT_ALLOWED")));
    }

    @Test
    void shouldHandleUnhandledExceptionWithGenericMessage() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/unhandled"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.error.message", is("An unexpected error occurred. Please try again later.")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(content().string(not(containsString("Simulated unexpected failure"))));
    }
}
