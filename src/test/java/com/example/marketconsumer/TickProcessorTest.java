package com.example.marketconsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.marketconsumer.db.TickRepository;
import com.example.marketconsumer.model.MarketTick;
import com.example.marketconsumer.service.TickProcessor;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TickProcessorTest {

  @Test
  void process_parsesValidJson() throws SQLException {
    CapturingTickRepository repository = new CapturingTickRepository();
    TickProcessor processor = new TickProcessor(repository);

    processor.process(
        """
                {
                  "ts": "2026-03-15T11:00:00Z",
                  "symbol": "USDJPY",
                  "price": 149.25
                }
                """);

    assertEquals(Instant.parse("2026-03-15T11:00:00Z"), repository.captured.ts());
    assertEquals("USDJPY", repository.captured.symbol());
    assertEquals(149.25, repository.captured.price());
  }

  @Test
  void process_throwsForInvalidJson() {
    CapturingTickRepository repository = new CapturingTickRepository();
    TickProcessor processor = new TickProcessor(repository);

    assertThrows(IllegalArgumentException.class, () -> processor.process("not-json"));
  }

  private static class CapturingTickRepository extends TickRepository {
    private MarketTick captured;

    CapturingTickRepository() {
      super("", "", "");
    }

    @Override
    public void insert(MarketTick tick) {
      this.captured = tick;
    }
  }
}
