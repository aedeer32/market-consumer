#!/bin/zsh

printf '{"ts":"2026-03-15T11:00:00Z","symbol":"USDJPY","price":149.25}\n{"ts":"2026-03-15T11:00:01Z","symbol":"USDJPY","price":149.27}\n' | \
docker exec -i <KAFKA_CONTAINER_ID> /opt/kafka/bin/kafka-console-producer.sh \
  --topic prices \
  --bootstrap-server localhost:9092