package com.soby.sequencer;

import static org.junit.jupiter.api.Assertions.*;

import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.event.OrderEvent.EventState;
import com.soby.sequencer.handler.OutputHandler;
import com.soby.sequencer.model.OrderType;
import com.soby.sequencer.model.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OutputHandlerTest {

  private OutputHandler handler;

  @BeforeEach
  public void setUp() {
    handler = new OutputHandler();
  }

  @Test
  public void testOnEventDoesNotThrow() {
    OrderEvent event = new OrderEvent();
    event.setOrderId(100L);
    event.setSymbol("AAPL");
    event.setSide(Side.BUY);
    event.setType(OrderType.LIMIT);
    event.setPrice(15000L);
    event.setQuantity(50L);
    event.setTimestampNanos(System.nanoTime());
    event.setState(EventState.PUBLISHED);

    assertDoesNotThrow(() -> handler.onEvent(event, 1L, false), "Should not throw");
  }

  @Test
  public void testOnEventCallsReset() {
    OrderEvent event = new OrderEvent();
    event.setOrderId(100L);
    event.setSymbol("AAPL");
    event.setSide(Side.BUY);
    event.setType(OrderType.LIMIT);
    event.setPrice(15000L);
    event.setQuantity(50L);
    event.setTimestampNanos(System.nanoTime());
    event.setState(EventState.PUBLISHED);

    handler.onEvent(event, 1L, false);

    assertEquals(EventState.EMPTY, event.getState(), "State should be reset to EMPTY");
  }

  @Test
  public void testOnEventResetsAllFields() {
    OrderEvent event = new OrderEvent();
    event.setOrderId(100L);
    event.setSymbol("AAPL");
    event.setSide(Side.BUY);
    event.setType(OrderType.LIMIT);
    event.setPrice(15000L);
    event.setQuantity(50L);
    event.setTimestampNanos(System.nanoTime());
    event.setSequenceNumber(999L);
    event.setState(EventState.PUBLISHED);

    handler.onEvent(event, 1L, false);

    assertEquals(0L, event.getSequenceNumber(), "Sequence should be reset");
    assertEquals(0L, event.getOrderId(), "OrderId should be reset");
    assertNull(event.getSymbol(), "Symbol should be reset");
    assertNull(event.getSide(), "Side should be reset");
    assertNull(event.getType(), "Type should be reset");
    assertEquals(0L, event.getPrice(), "Price should be reset");
    assertEquals(0L, event.getQuantity(), "Quantity should be reset");
    assertEquals(0L, event.getTimestampNanos(), "Timestamp should be reset");
    assertEquals(EventState.EMPTY, event.getState(), "State should be reset");
  }

  @Test
  public void testOnEventWithEndOfBatchFlag() {
    OrderEvent event = new OrderEvent();
    event.setOrderId(100L);
    event.setState(EventState.PUBLISHED);

    assertDoesNotThrow(() -> handler.onEvent(event, 1L, true), "Should handle endOfBatch=true");
    assertDoesNotThrow(() -> handler.onEvent(event, 1L, false), "Should handle endOfBatch=false");
  }

  @Test
  public void testOnEventWithZeroValues() {
    OrderEvent event = new OrderEvent();
    event.setOrderId(0L);
    event.setSymbol("");
    event.setSide(Side.SELL);
    event.setType(OrderType.MARKET);
    event.setPrice(0L);
    event.setQuantity(0L);
    event.setTimestampNanos(System.nanoTime());
    event.setState(EventState.PUBLISHED);

    assertDoesNotThrow(() -> handler.onEvent(event, 0L, false), "Should handle zero values");
  }
}
