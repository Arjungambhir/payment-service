package com.eccom.paymentservice.service;

import java.util.UUID;

public class RefundNotFoundException extends RuntimeException {

    public RefundNotFoundException(UUID id) {
        super("Refund " + id + " was not found");
    }
}
