package com.eccom.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eccom.paymentservice.api.CreatePaymentRequest;
import com.eccom.paymentservice.api.PaymentResponse;
import com.eccom.paymentservice.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    void createsPendingPaymentAndRetrievesIt() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("19.99"), "USD", "Order 123");

        PaymentResponse created = paymentService.create(request);
        PaymentResponse retrieved = paymentService.get(created.id());

        assertNotNull(created.id());
        assertNotNull(created.createdAt());
        assertEquals(new BigDecimal("19.99"), created.amount());
        assertEquals("USD", created.currency());
        assertEquals("Order 123", created.description());
        assertEquals(PaymentStatus.PENDING, created.status());
        assertEquals(created, retrieved);
    }

    @Test
    void createsDistinctPaymentIds() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("1.00"), "USD", null);

        PaymentResponse first = paymentService.create(request);
        PaymentResponse second = paymentService.create(request);

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void marksPendingPaymentAsFailed() {
        PaymentResponse created = paymentService.create(new CreatePaymentRequest(
                new BigDecimal("4.25"), "EUR", null));

        PaymentResponse failed = paymentService.markFailed(created.id());

        assertEquals(PaymentStatus.FAILED, failed.status());
        assertEquals(failed, paymentService.get(created.id()));
        assertEquals(created.amount(), failed.amount());
        assertEquals(created.currency(), failed.currency());
    }

    @Test
    void cannotMarkPaymentFailedMoreThanOnce() {
        PaymentResponse created = paymentService.create(new CreatePaymentRequest(
                new BigDecimal("4.25"), "EUR", null));
        paymentService.markFailed(created.id());

        assertThrows(InvalidPaymentStateException.class,
                () -> paymentService.markFailed(created.id()));
    }

    @Test
    void throwsWhenPaymentDoesNotExist() {
        assertThrows(PaymentNotFoundException.class, () -> paymentService.get(UUID.randomUUID()));
        assertThrows(PaymentNotFoundException.class, () -> paymentService.markFailed(UUID.randomUUID()));
    }
}
