package com.eccom.paymentservice.service;

import com.eccom.paymentservice.api.CreatePaymentRequest;
import com.eccom.paymentservice.api.PaymentResponse;
import com.eccom.paymentservice.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final ConcurrentMap<UUID, PaymentResponse> payments = new ConcurrentHashMap<>();

    public PaymentResponse create(CreatePaymentRequest request) {
        PaymentResponse payment = new PaymentResponse(
                UUID.randomUUID(),
                request.amount(),
                request.currency(),
                request.description(),
                PaymentStatus.PENDING,
                Instant.now());
        payments.put(payment.id(), payment);
        return payment;
    }

    public PaymentResponse get(UUID id) {
        PaymentResponse payment = payments.get(id);
        if (payment == null) {
            throw new PaymentNotFoundException(id);
        }
        return payment;
    }

    public PaymentResponse markFailed(UUID id) {
        return payments.compute(id, (paymentId, payment) -> {
            if (payment == null) {
                throw new PaymentNotFoundException(paymentId);
            }
            if (payment.status() != PaymentStatus.PENDING) {
                throw new InvalidPaymentStateException(paymentId, payment.status(), PaymentStatus.PENDING);
            }
            return new PaymentResponse(
                    payment.id(),
                    payment.amount(),
                    payment.currency(),
                    payment.description(),
                    PaymentStatus.FAILED,
                    payment.createdAt());
        });
    }
}
