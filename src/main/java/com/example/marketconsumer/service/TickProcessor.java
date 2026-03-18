package com.example.marketconsumer.service;

import com.example.marketconsumer.db.TickRepository;
import com.example.marketconsumer.model.MarketTick;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.time.Instant;

public class TickProcessor {
  private final TickRepository repository;
  private final ObjectMapper objectMapper;

  public TickProcessor(TickRepository repository) {
    this.repository = repository;
    this.objectMapper = new ObjectMapper();
  }

  public void process(String message) throws SQLException {
    try {
      JsonNode node = objectMapper.readTree(message);

      MarketTick tick =
          new MarketTick(
              Instant.parse(requiredText(node, "ts")),
              requiredText(node, "symbol"),
              requiredDouble(node, "price"));

      repository.insert(tick);
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to process message: " + message, e);
    }
  }

  private String requiredText(JsonNode node, String fieldName) {
    JsonNode value = node.get(fieldName);
    if (value == null || value.isNull() || value.asText().isBlank()) {
      throw new IllegalArgumentException("Missing or blank field: " + fieldName);
    }
    return value.asText();
  }

  private double requiredDouble(JsonNode node, String fieldName) {
    JsonNode value = node.get(fieldName);
    if (value == null || value.isNull()) {
      throw new IllegalArgumentException("Missing field: " + fieldName);
    }
    return value.asDouble();
  }
}
