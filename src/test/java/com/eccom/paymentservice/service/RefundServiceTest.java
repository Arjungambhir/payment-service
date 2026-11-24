package com.eccom.paymentservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eccom.paymentservice.api.CreatePaymentRequest;
import com.eccom.paymentservice.api.PaymentResponse;
import com.eccom.paymentservice.api.RefundResponse;
import com.eccom.paymentservice.domain.RefundStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefundServiceTest {

    private PaymentService paymentService;
    private RefundService refundService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        refundService = new RefundService(paymentService);
    }

    @Test
    void createsFullRefundForFailedPaymentAndRetrievesIt() {
        PaymentResponse payment = createFailedPayment();

        RefundResponse refund = refundService.create(payment.id());

        assertNotNull(refund.id());
        assertNotNull(refund.createdAt());
        assertEquals(payment.id(), refund.paymentId());
        assertEquals(payment.amount(), refund.amount());
        assertEquals(payment.currency(), refund.currency());
        assertEquals(RefundStatus.PENDING, refund.status());
        assertEquals(refund, refundService.get(refund.id()));
    }

    @Test
    void doesNotAllowRefundForPendingPayment() {
        PaymentResponse payment = paymentService.create(new CreatePaymentRequest(
                new BigDecimal("9.00"), "USD", null));

        assertThrows(InvalidPaymentStateException.class, () -> refundService.create(payment.id()));
    }

    @Test
    void allowsOnlyOneRefundPerPayment() {
        PaymentResponse payment = createFailedPayment();
        refundService.create(payment.id());

        assertThrows(RefundAlreadyExistsException.class, () -> refundService.create(payment.id()));
    }

    @Test
    void throwsWhenPaymentDoesNotExist() {
        assertThrows(PaymentNotFoundException.class, () -> refundService.create(UUID.randomUUID()));
    }

    @Test
    void throwsWhenRefundDoesNotExist() {
        assertThrows(RefundNotFoundException.class, () -> refundService.get(UUID.randomUUID()));
    }

    private PaymentResponse createFailedPayment() {
        PaymentResponse payment = paymentService.create(new CreatePaymentRequest(
                new BigDecimal("19.99"), "USD", "Order 123"));
        return paymentService.markFailed(payment.id());
    }
}
