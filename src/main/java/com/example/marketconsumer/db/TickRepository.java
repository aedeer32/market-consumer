package com.example.marketconsumer.db;

import com.example.marketconsumer.model.MarketTick;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TickRepository {
  private final String url;
  private final String user;
  private final String password;

  public TickRepository(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public void insert(MarketTick tick) throws SQLException {
    String sql =
        """
                INSERT INTO market_ticks (ts, symbol, price)
                VALUES (?, ?, ?)
                """;

    try (Connection conn = DriverManager.getConnection(url, user, password);
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setTimestamp(1, java.sql.Timestamp.from(tick.ts()));
      ps.setString(2, tick.symbol());
      ps.setDouble(3, tick.price());
      ps.executeUpdate();
    }
  }
}
