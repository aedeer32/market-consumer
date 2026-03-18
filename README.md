# market-consumer

Kafka topic `prices` から JSON メッセージを読み取り、PostgreSQL の `market_ticks` テーブルへ insert する最小 Java consumer。

## 前提

- Java 17
- Maven
- Kafka が `localhost:9092` で起動済み
- PostgreSQL が `localhost:5433` で起動済み
- DB: `appdb`
- User: `appuser`
- Password: `apppass`

## テーブル作成

```bash
psql -h localhost -p 5433 -U appuser -d appdb -f scripts/create_table.sql
```

---

## 最初にやるコマンド

```bash
psql -h localhost -p 5433 -U appuser -d appdb -f scripts/create_table.sql
mvn clean test package
java -jar target/market-consumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## 別ターミナルでKafkaに送信

```aiignore
printf '{"ts":"2026-03-15T11:00:00Z","symbol":"USDJPY","price":149.25}\n{"ts":"2026-03-15T11:00:01Z","symbol":"USDJPY","price":149.27}\n' | \
docker exec -i <KAFKA_CONTAINER_ID> /opt/kafka/bin/kafka-console-producer.sh \
--topic prices \
--bootstrap-server localhost:9092
```
