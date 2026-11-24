package com.eccom.paymentservice.api;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.eccom.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentService paymentService;

    @Test
    void createsAndRetrievesPayment() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":19.99,"currency":"USD","description":"Order 123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/payments/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(19.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.description").value("Order 123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createsPaymentWithoutOptionalDescription() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1.00,"currency":"EUR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.amount").value(1.0))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"amount\":0,\"currency\":\"USD\"}",
            "{\"amount\":-1,\"currency\":\"USD\"}",
            "{\"amount\":1.001,\"currency\":\"USD\"}",
            "{\"amount\":1,\"currency\":\"usd\"}",
            "{\"amount\":1,\"currency\":\"US\"}",
            "{\"amount\":1,\"currency\":\"   \"}",
            "{\"currency\":\"USD\"}"
    })
    void rejectsInvalidPaymentRequests(String request) throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void rejectsDescriptionLongerThan255Characters() throws Exception {
        String description = "x".repeat(256);
        String request = "{\"amount\":1,\"currency\":\"USD\",\"description\":\""
                + description + "\"}";

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void returnsNotFoundForUnknownPayment() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", "e1530b6d-e5a5-43bb-9140-e5fc0b55c6e8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payment not found"));
    }

    @Test
    void returnsBadRequestForMalformedPaymentId() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createsAndRetrievesRefundForFailedPayment() throws Exception {
        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":19.99,"currency":"USD","description":"Failed order"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = JsonPath.read(paymentResult.getResponse().getContentAsString(), "$.id");
        paymentService.markFailed(java.util.UUID.fromString(paymentId));

        MvcResult refundResult = mockMvc.perform(post("/api/v1/payments/{id}/refunds", paymentId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/v1/refunds/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.amount").value(19.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String refundId = JsonPath.read(refundResult.getResponse().getContentAsString(), "$.id");
        mockMvc.perform(get("/api/v1/refunds/{id}", refundId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(refundId))
                .andExpect(jsonPath("$.paymentId").value(paymentId));
    }

    @Test
    void rejectsRefundForPaymentThatHasNotFailed() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":5.00,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/payments/{id}/refunds", paymentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Payment state conflict"));
    }

    @Test
    void rejectsDuplicateRefundRequests() throws Exception {
        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":5.00,"currency":"USD"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String paymentId = JsonPath.read(paymentResult.getResponse().getContentAsString(), "$.id");
        paymentService.markFailed(java.util.UUID.fromString(paymentId));

        mockMvc.perform(post("/api/v1/payments/{id}/refunds", paymentId))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/payments/{id}/refunds", paymentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Payment state conflict"));
    }

    @Test
    void returnsNotFoundForRefundOfUnknownPayment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{id}/refunds", "e1530b6d-e5a5-43bb-9140-e5fc0b55c6e8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payment not found"));
    }
}
