package com.eccom.paymentservice.service;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID id) {
        super("Payment " + id + " was not found");
    }
}
