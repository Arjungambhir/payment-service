package com.eccom.paymentservice.service;

import java.util.UUID;

public class RefundAlreadyExistsException extends RuntimeException {

    public RefundAlreadyExistsException(UUID paymentId) {
        super("A refund has already been requested for payment " + paymentId);
    }
}
