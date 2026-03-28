SHELL := /bin/bash

APP_NAME := market-consumer
JAR := target/market-consumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar

DB_HOST := localhost
DB_PORT := 5433
DB_NAME := appdb
DB_USER := appuser
DB_PASSWORD := apppass

KAFKA_BOOTSTRAP := localhost:9092
KAFKA_TOPIC := prices
KAFKA_CONTAINER := market-consumer-kafka

export PGPASSWORD := $(DB_PASSWORD)

.PHONY: help up down restart logs ps wait-db wait-kafka init-db build test run dev clean \
        kafka-producer kafka-topic-list db-shell db-check consume-check reset

help:
	@echo "Available targets:"
	@echo "  make up            # Start PostgreSQL and Kafka with docker compose"
	@echo "  make down          # Stop containers"
	@echo "  make restart       # Restart containers"
	@echo "  make ps            # Show container status"
	@echo "  make logs          # Tail compose logs"
	@echo "  make wait-db       # Wait until PostgreSQL is ready"
	@echo "  make wait-kafka    # Wait until Kafka is ready"
	@echo "  make init-db       # Create market_ticks table"
	@echo "  make build         # Build jar with Maven"
	@echo "  make test          # Run tests"
	@echo "  make run           # Run consumer jar"
	@echo "  make dev           # up + wait + init-db + build"
	@echo "  make kafka-producer# Open interactive Kafka producer"
	@echo "  make kafka-topic-list # List Kafka topics"
	@echo "  make db-shell      # Open psql shell"
	@echo "  make db-check      # Query latest rows from market_ticks"
	@echo "  make consume-check # End-to-end quick check"
	@echo "  make clean         # Remove Maven target directory"
	@echo "  make reset         # Stop containers and delete volumes"

up:
	docker compose up -d

down:
	docker compose down

restart: down up

ps:
	docker compose ps

logs:
	docker compose logs -f

wait-db:
	@echo "Waiting for PostgreSQL on $(DB_HOST):$(DB_PORT)..."
	@until pg_isready -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) >/dev/null 2>&1; do \
		sleep 1; \
	done
	@echo "PostgreSQL is ready."

wait-kafka:
	@echo "Waiting for Kafka on $(KAFKA_BOOTSTRAP)..."
	@until docker exec $(KAFKA_CONTAINER) /opt/bitnami/kafka/bin/kafka-topics.sh \
		--bootstrap-server $(KAFKA_BOOTSTRAP) --list >/dev/null 2>&1; do \
		sleep 2; \
	done
	@echo "Kafka is ready."

init-db:
	psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f scripts/create_table.sql

build:
	mvn clean test package

test:
	mvn test

run:
	java -jar $(JAR)

dev: up wait-db wait-kafka init-db build
	@echo "Environment is ready."
	@echo "Next step: make run"

clean:
	mvn clean

kafka-producer:
	docker exec -it $(KAFKA_CONTAINER) /opt/bitnami/kafka/bin/kafka-console-producer.sh \
		--bootstrap-server $(KAFKA_BOOTSTRAP) \
		--topic $(KAFKA_TOPIC)

kafka-topic-list:
	docker exec -it $(KAFKA_CONTAINER) /opt/bitnami/kafka/bin/kafka-topics.sh \
		--bootstrap-server $(KAFKA_BOOTSTRAP) \
		--list

db-shell:
	psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME)

db-check:
	psql -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) \
		-c "SELECT * FROM market_ticks ORDER BY id DESC LIMIT 10;"

consume-check:
	@echo "Sending one sample message to Kafka topic $(KAFKA_TOPIC)..."
	@printf '{"ts":"2026-03-20T11:00:00Z","symbol":"USDJPY","price":149.25}\n' | \
	docker exec -i $(KAFKA_CONTAINER) /opt/bitnami/kafka/bin/kafka-console-producer.sh \
		--bootstrap-server $(KAFKA_BOOTSTRAP) \
		--topic $(KAFKA_TOPIC)
	@echo "Sent. Check the consumer log and then run: make db-check"

reset:
	docker compose down -v