package com.eccom.paymentservice.api;

import com.eccom.paymentservice.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        Instant createdAt) {
}
