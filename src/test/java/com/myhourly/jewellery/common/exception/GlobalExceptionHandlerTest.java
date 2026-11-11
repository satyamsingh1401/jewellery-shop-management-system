package com.myhourly.jewellery.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ProbeController {
        @GetMapping("/probe/{id}/availability")
        public int availability(@PathVariable Long id, @RequestParam int quantity) {
            return quantity;
        }

        @GetMapping("/probe/insufficient")
        public int insufficient() {
            throw new InsufficientStockException("Insufficient stock for product 1 (requested 100)");
        }

        @GetMapping("/probe/invalid")
        public int invalid() {
            throw new BusinessValidationException("quantityChange must not be zero");
        }

        @GetMapping("/probe/sorted")
        public int sorted() {
            throw new PropertyReferenceException("doesNotExist", TypeInformation.of(ProbeController.class), List.of());
        }

        @PostMapping(value = "/probe/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> json(@RequestBody Map<String, Object> body) {
            return body;
        }

        @GetMapping(value = "/probe/text", produces = MediaType.TEXT_PLAIN_VALUE)
        public String text() {
            return "ok";
        }

        @GetMapping("/probe/missing")
        public int missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/probe/missing", "probe/missing");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingQuantityReturnsFullErrorFormat() throws Exception {
        mockMvc.perform(get("/probe/1/availability"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/probe/1/availability"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity is required"));
    }

    @Test
    void nonNumericQuantityReturnsFullErrorFormat() throws Exception {
        mockMvc.perform(get("/probe/1/availability").param("quantity", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/probe/1/availability"))
                .andExpect(jsonPath("$.fieldErrors.quantity").value("quantity must be a valid int"));
    }

    @Test
    void insufficientStockReturns409() throws Exception {
        mockMvc.perform(get("/probe/insufficient"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product 1 (requested 100)"))
                .andExpect(jsonPath("$.path").value("/probe/insufficient"));
    }

    @Test
    void otherBusinessValidationStillReturns400() throws Exception {
        mockMvc.perform(get("/probe/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("quantityChange must not be zero"));
    }

    @Test
    void nonNumericPathVariableReturnsFullErrorFormat() throws Exception {
        mockMvc.perform(get("/probe/abc/availability").param("quantity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.id").value("id must be a valid Long"));
    }

    @Test
    void unknownSortPropertyReturns400WithSortFieldError() throws Exception {
        mockMvc.perform(get("/probe/sorted"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/probe/sorted"))
                .andExpect(jsonPath("$.fieldErrors.sort").value("sort must reference a supported property"))
                .andExpect(content().string(not(containsString("doesNotExist"))))
                .andExpect(content().string(not(containsString("ProbeController"))));
    }

    @Test
    void unsupportedMethodReturns405WithAllowHeader() throws Exception {
        mockMvc.perform(delete("/probe/insufficient"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("HTTP method DELETE is not supported for this endpoint"))
                .andExpect(jsonPath("$.path").value("/probe/insufficient"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void unsupportedContentTypeReturns415WithAcceptHeader() throws Exception {
        mockMvc.perform(post("/probe/json").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", containsString("application/json")))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.message").value(containsString("application/json")))
                .andExpect(jsonPath("$.path").value("/probe/json"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void notAcceptableReturns406InStandardFormat() throws Exception {
        mockMvc.perform(get("/probe/text").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.error").value("Not Acceptable"))
                .andExpect(jsonPath("$.message").value(containsString("text/plain")))
                .andExpect(jsonPath("$.path").value("/probe/text"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void unknownRouteReturns404InStandardFormat() throws Exception {
        mockMvc.perform(get("/probe/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No endpoint matches the requested path"))
                .andExpect(jsonPath("$.path").value("/probe/missing"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }
}
