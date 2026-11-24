# Payment Service

A Java 21 and Spring Boot starter service for the Eccom application. It exposes
a small payment API and an Actuator health endpoint.

## Requirements

- Java 21
- Maven 3.6.3 or later

## Run

```sh
mvn spring-boot:run
```

The service listens on port `8080`. Check health at
`http://localhost:8080/actuator/health`.

## API

Create a payment:

```sh
curl -i -X POST http://localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{"amount":19.99,"currency":"USD","description":"Order 123"}'
```

The service responds with `201 Created`, a `Location` header, and a payment
resource containing its generated ID, the submitted amount/currency/description,
creation time, and `PENDING` status.

Retrieve a payment:

```sh
curl http://localhost:8080/api/v1/payments/<payment-id>
```

Request a full refund for a failed payment:

```sh
curl -i -X POST http://localhost:8080/api/v1/payments/<payment-id>/refunds
```

The payment must have been marked `FAILED` by the payment-processing
integration. The refund is created with `PENDING` status and can be retrieved
from `/api/v1/refunds/<refund-id>`. A payment may have only one refund request.
The scaffold does not expose a public endpoint for changing payment status;
provider integration should call the payment service when it reports failure.

Amounts must be positive and have at most two decimal places. Currency is
currently checked for the shape of a three-letter uppercase code, not against an
ISO currency registry. Invalid requests return `400 Bad Request`; unknown
payment or refund IDs return `404 Not Found`; refunds requested for payments
that are not failed, or duplicate refund requests, return `409 Conflict`.

This is a scaffold only: payments are held in memory and disappear when the
service restarts. Refunds are queued in memory only; no payment is authorized,
captured, or sent to a provider, and no refund is sent to a payment provider.
