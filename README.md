# market-consumer

A minimal Java Kafka consumer that reads JSON price events from a Kafka topic and inserts them into PostgreSQL.

---

## Overview

This application:

- Consumes messages from Kafka topic `prices`
- Parses JSON messages of the form:

```json
{"ts":"2026-03-20T11:00:00Z","symbol":"USDJPY","price":149.25}
```

- Inserts them into PostgreSQL table `market_ticks`

---

## Architecture

Kafka (localhost:9092)
        ↓
market-consumer (Java)
        ↓
PostgreSQL (localhost:5433)

---

## Prerequisites

Make sure you have the following installed:

- Docker
- Docker Compose
- Java 17+
- Maven
- Make

---

## Quick Start (Recommended)

### 1. Start infrastructure

```bash
make up
```

### 2. Wait until services are ready

```bash
make wait-db
make wait-kafka
```

### 3. Initialize database

```bash
make init-db
```

### 4. Build the application

```bash
make build
```

### 5. Run the consumer

```bash
make run
```

---

## One-line setup (faster)

```bash
make dev
make run
```

---

## Sending test data

Open another terminal:

```bash
make kafka-producer
```

Then input messages:

```text
{"ts":"2026-03-20T11:00:00Z","symbol":"USDJPY","price":149.25}
{"ts":"2026-03-20T11:00:01Z","symbol":"USDJPY","price":149.27}
```

---

## Verify data in PostgreSQL

```bash
make db-check
```

---

## Available Make Commands

```bash
make help
```

---

## Configuration

| Component   | Value |
|------------|------|
| Kafka      | localhost:9092 |
| PostgreSQL | localhost:5433 |
| DB name    | appdb |
| User       | appuser |
| Password   | apppass |
| Topic      | prices |

---

## Database Schema

```sql
CREATE TABLE IF NOT EXISTS market_ticks (
  id SERIAL PRIMARY KEY,
  ts TIMESTAMP NOT NULL,
  symbol TEXT NOT NULL,
  price DOUBLE PRECISION NOT NULL
);
```

---

## Troubleshooting

### Kafka connection issue

If you see logs like:

Rebootstrapping with [localhost:9092]

Kafka is not ready. Run:

```bash
make wait-kafka
```

---

### PostgreSQL connection issue

```bash
make wait-db
```

---

### Reset environment

```bash
make reset
```

---

## License

MIT
