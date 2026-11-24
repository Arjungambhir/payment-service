package com.eccom.paymentservice.service;

import com.eccom.paymentservice.api.PaymentResponse;
import com.eccom.paymentservice.api.RefundResponse;
import com.eccom.paymentservice.domain.PaymentStatus;
import com.eccom.paymentservice.domain.RefundStatus;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class RefundService {

    private final PaymentService paymentService;
    private final ConcurrentMap<UUID, RefundResponse> refunds = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> refundIdsByPaymentId = new ConcurrentHashMap<>();

    public RefundService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public synchronized RefundResponse create(UUID paymentId) {
        if (refundIdsByPaymentId.containsKey(paymentId)) {
            throw new RefundAlreadyExistsException(paymentId);
        }

        PaymentResponse payment = paymentService.get(paymentId);
        if (payment.status() != PaymentStatus.FAILED) {
            throw new InvalidPaymentStateException(paymentId, payment.status(), PaymentStatus.FAILED);
        }

        RefundResponse refund = new RefundResponse(
                UUID.randomUUID(),
                payment.id(),
                payment.amount(),
                payment.currency(),
                RefundStatus.PENDING,
                Instant.now());
        refunds.put(refund.id(), refund);
        refundIdsByPaymentId.put(paymentId, refund.id());
        return refund;
    }

    public RefundResponse get(UUID id) {
        RefundResponse refund = refunds.get(id);
        if (refund == null) {
            throw new RefundNotFoundException(id);
        }
        return refund;
    }
}
