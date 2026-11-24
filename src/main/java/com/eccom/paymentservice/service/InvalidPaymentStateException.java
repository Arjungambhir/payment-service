package com.eccom.paymentservice.service;

import com.eccom.paymentservice.domain.PaymentStatus;
import java.util.UUID;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(UUID id, PaymentStatus actual, PaymentStatus expected) {
        super("Payment " + id + " has status " + actual + "; expected " + expected);
    }
}
