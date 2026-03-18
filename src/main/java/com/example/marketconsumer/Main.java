package com.example.marketconsumer;

import com.example.marketconsumer.config.AppConfig;
import com.example.marketconsumer.consumer.PriceConsumer;
import com.example.marketconsumer.db.TickRepository;
import com.example.marketconsumer.service.TickProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    AppConfig config = AppConfig.fromEnv();

    logger.info("Starting market-consumer");
    logger.info("Kafka bootstrap servers={}", config.kafkaBootstrapServers());
    logger.info("Kafka topic={}", config.kafkaTopic());
    logger.info("Kafka group id={}", config.kafkaGroupId());
    logger.info("Postgres url={}", config.postgresUrl());

    TickRepository repository =
        new TickRepository(config.postgresUrl(), config.postgresUser(), config.postgresPassword());

    TickProcessor processor = new TickProcessor(repository);

    PriceConsumer consumer =
        new PriceConsumer(
            config.kafkaBootstrapServers(), config.kafkaTopic(), config.kafkaGroupId(), processor);

    consumer.run();
  }
}
