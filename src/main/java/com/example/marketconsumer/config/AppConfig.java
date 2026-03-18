package com.example.marketconsumer.config;

public class AppConfig {
  private final String kafkaBootstrapServers;
  private final String kafkaTopic;
  private final String kafkaGroupId;
  private final String postgresUrl;
  private final String postgresUser;
  private final String postgresPassword;

  public AppConfig(
      String kafkaBootstrapServers,
      String kafkaTopic,
      String kafkaGroupId,
      String postgresUrl,
      String postgresUser,
      String postgresPassword) {
    this.kafkaBootstrapServers = kafkaBootstrapServers;
    this.kafkaTopic = kafkaTopic;
    this.kafkaGroupId = kafkaGroupId;
    this.postgresUrl = postgresUrl;
    this.postgresUser = postgresUser;
    this.postgresPassword = postgresPassword;
  }

  public static AppConfig fromEnv() {
    return new AppConfig(
        getenvOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        getenvOrDefault("KAFKA_TOPIC", "prices"),
        getenvOrDefault("KAFKA_GROUP_ID", "market-consumer-group"),
        getenvOrDefault("POSTGRES_URL", "jdbc:postgresql://localhost:5433/appdb"),
        getenvOrDefault("POSTGRES_USER", "appuser"),
        getenvOrDefault("POSTGRES_PASSWORD", "apppass"));
  }

  private static String getenvOrDefault(String key, String defaultValue) {
    String value = System.getenv(key);
    return (value == null || value.isBlank()) ? defaultValue : value;
  }

  public String kafkaBootstrapServers() {
    return kafkaBootstrapServers;
  }

  public String kafkaTopic() {
    return kafkaTopic;
  }

  public String kafkaGroupId() {
    return kafkaGroupId;
  }

  public String postgresUrl() {
    return postgresUrl;
  }

  public String postgresUser() {
    return postgresUser;
  }

  public String postgresPassword() {
    return postgresPassword;
  }
}
