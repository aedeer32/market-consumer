# market-consumer

A minimal Java Kafka consumer that reads JSON price events from a Kafka topic and inserts them into PostgreSQL.

## Overview

This application:

- Consumes messages from Kafka topic `prices`
- Parses JSON messages such as `{"ts":"2026-03-20T11:00:00Z","symbol":"USDJPY","price":149.25}`
- Inserts them into PostgreSQL table `market_ticks`

## Local Development

### Prerequisites

- Docker
- Docker Compose
- Java 17+
- Maven
- Make

### Local architecture

```text
Kafka (localhost:9092)
        ↓
market-consumer (Java)
        ↓
PostgreSQL (localhost:5433)
```

### Local configuration defaults

| Component | Value |
| --- | --- |
| Kafka | `localhost:9092` |
| PostgreSQL | `localhost:5433` |
| DB name | `appdb` |
| User | `appuser` |
| Password | `apppass` |
| Topic | `prices` |

These values come from the application's environment variable defaults and are intended for local use only.

### Quick start

1. Start infrastructure.

```bash
make up
```

2. Wait until PostgreSQL and Kafka are ready.

```bash
make wait-db
make wait-kafka
```

3. Create the database table.

```bash
make init-db
```

4. Build the application.

```bash
make build
```

5. Run the consumer.

```bash
make run
```

You can also use the shorter setup flow:

```bash
make dev
make run
```

### Useful local commands

- `make help`
- `make kafka-producer`
- `make db-check`
- `make consume-check`
- `make reset`

### Database schema

```sql
CREATE TABLE IF NOT EXISTS market_ticks (
  id SERIAL PRIMARY KEY,
  ts TIMESTAMP NOT NULL,
  symbol TEXT NOT NULL,
  price DOUBLE PRECISION NOT NULL
);
```

## Jenkins Deployment

### Deployment model

The repository is set up for JAR-based deployment:

1. Jenkins checks out the repository.
2. Jenkins runs `mvn -B clean verify`.
3. Maven tests run and `spotless:check` runs during the `verify` phase.
4. Jenkins archives `target/*-jar-with-dependencies.jar`.
5. On the `main` branch, Jenkins copies the JAR and `scripts/deploy.sh` to the target host over SSH.
6. `deploy.sh` switches `current/app.jar`, restarts the process, and rolls back if the new process does not stay up.

### CI format check

Formatting is enforced in CI through Spotless in [`pom.xml`](/Users/pivot19/Development/market-consumer/pom.xml). The Jenkins pipeline runs `mvn -B clean verify`, so a formatting violation fails the build before artifact archive or deploy.

### Jenkins credentials and configuration

The current pipeline expects:

- SSH credential id: `market-consumer-deploy-key`
- Deployment host values in [`Jenkinsfile`](/Users/pivot19/Development/market-consumer/Jenkinsfile):
  - `DEPLOY_HOST`
  - `DEPLOY_PORT`
  - `DEPLOY_USER`
  - `DEPLOY_BASE_DIR`

Recommended Jenkins setup:

- Keep `market-consumer-deploy-key` in Jenkins Credentials.
- Move `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, and `DEPLOY_BASE_DIR` from inline defaults to folder-level environment variables or pipeline parameters before production use.
- Use a Multibranch Pipeline so `main` deploys automatically and other branches only run build and verification.

### Target host directory layout

The deployment target is expected to use a layout like this:

```text
/opt/market-consumer/
  bin/
    deploy.sh
  current/
    app.jar -> /opt/market-consumer/releases/<build>/<jar-file>
  releases/
    <build>/
      market-consumer-...-jar-with-dependencies.jar
  shared/
    app.env
    logs/
    run/
```

### Required environment on the target host

Before Jenkins deploys, create `shared/app.env` under the deployment base directory. Example:

```bash
KAFKA_BOOTSTRAP_SERVERS=broker01.example.internal:9092
KAFKA_TOPIC=prices
KAFKA_GROUP_ID=market-consumer-group
POSTGRES_URL=jdbc:postgresql://db01.example.internal:5432/appdb
POSTGRES_USER=appuser
POSTGRES_PASSWORD=change-me
JAVA_OPTS=-Xms256m -Xmx512m
```

The host also needs:

- Java 17 runtime available as `java`, or `JAVA_BIN` set appropriately
- Write permission to `DEPLOY_BASE_DIR`
- SSH access from Jenkins using the configured credential

### `deploy.sh` responsibilities

[`scripts/deploy.sh`](/Users/pivot19/Development/market-consumer/scripts/deploy.sh) is responsible for:

- Verifying that the new JAR exists
- Verifying that `shared/app.env` exists
- Stopping the currently running process using the PID file
- Switching `current/app.jar` to the new release
- Loading environment variables from `shared/app.env`
- Starting the new process with `nohup`
- Writing the new PID to `shared/run/<app>.pid`
- Rolling back to the previous JAR if the new process exits during startup

### Rollback procedure

Automatic rollback:

- `deploy.sh` stores the previous `current/app.jar` symlink target
- If the new process fails to stay up, the script restores the previous symlink and starts the old JAR again

Manual rollback:

1. Log in to the target host.
2. Identify the previous release JAR under `releases/`.
3. Re-point `current/app.jar` to the previous JAR.
4. Stop the current PID if needed.
5. Start the previous JAR again using the same environment file.

Example:

```bash
ln -sfn /opt/market-consumer/releases/<previous-build>/<jar-file> /opt/market-consumer/current/app.jar
pkill -f 'market-consumer'
set -a
source /opt/market-consumer/shared/app.env
set +a
nohup java ${JAVA_OPTS:-} -jar /opt/market-consumer/current/app.jar \
  >>/opt/market-consumer/shared/logs/market-consumer.log 2>&1 &
echo $! >/opt/market-consumer/shared/run/market-consumer.pid
```

### Troubleshooting

Kafka connection issue:

- Verify `KAFKA_BOOTSTRAP_SERVERS` in `shared/app.env`
- For local development, run `make wait-kafka`

PostgreSQL connection issue:

- Verify `POSTGRES_URL`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` in `shared/app.env`
- For local development, run `make wait-db`

Build fails on formatting:

- Run `mvn spotless:apply`
- Commit the formatting changes and re-run Jenkins

## License

MIT
