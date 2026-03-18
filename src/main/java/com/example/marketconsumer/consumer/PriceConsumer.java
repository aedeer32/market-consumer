package com.example.marketconsumer.consumer;

import com.example.marketconsumer.service.TickProcessor;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PriceConsumer {
  private static final Logger logger = LoggerFactory.getLogger(PriceConsumer.class);

  private final String bootstrapServers;
  private final String topic;
  private final String groupId;
  private final TickProcessor processor;

  public PriceConsumer(
      String bootstrapServers, String topic, String groupId, TickProcessor processor) {
    this.bootstrapServers = bootstrapServers;
    this.topic = topic;
    this.groupId = groupId;
    this.processor = processor;
  }

  public void run() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    logger.info("Shutdown requested. Waking up Kafka consumer.");
                    consumer.wakeup();
                  }));

      consumer.subscribe(List.of(topic));
      logger.info("Subscribed to topic={}", topic);

      while (true) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(1))) {
          logger.info(
              "Received message topic={} partition={} offset={} value={}",
              record.topic(),
              record.partition(),
              record.offset(),
              record.value());
          processor.process(record.value());
        }
      }
    } catch (WakeupException e) {
      logger.info("Kafka consumer stopped.");
    } catch (Exception e) {
      logger.error("Consumer terminated unexpectedly.", e);
      throw new RuntimeException("Consumer terminated unexpectedly", e);
    }
  }
}
