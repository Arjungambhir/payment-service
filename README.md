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

Amounts must be positive and have at most two decimal places. Currency is
currently checked for the shape of a three-letter uppercase code, not against an
ISO currency registry. Invalid requests return `400 Bad Request`; unknown
payment IDs return `404 Not Found`.

This is a scaffold only: payments are held in memory and disappear when the
service restarts. No payment is authorized, captured, or sent to a provider.
