package com.soby.sequencer;

import static org.junit.jupiter.api.Assertions.*;

import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.event.OrderEventFactory;
import org.junit.jupiter.api.Test;

public class OrderEventFactoryTest {

  @Test
  public void testNewInstanceReturnsOrderEvent() {
    OrderEventFactory factory = new OrderEventFactory();
    OrderEvent event = factory.newInstance();

    assertNotNull(event, "Should return OrderEvent instance");
  }

  @Test
  public void testNewInstanceReturnsNewObject() {
    OrderEventFactory factory = new OrderEventFactory();
    OrderEvent event1 = factory.newInstance();
    OrderEvent event2 = factory.newInstance();

    assertNotSame(event1, event2, "Should return different instances");
  }

  @Test
  public void testNewInstanceReturnsCleanEvent() {
    OrderEventFactory factory = new OrderEventFactory();
    OrderEvent event = factory.newInstance();

    assertEquals(0L, event.getSequenceNumber(), "Sequence should be 0");
    assertEquals(0L, event.getOrderId(), "OrderId should be 0");
    assertNull(event.getSymbol(), "Symbol should be null");
    assertNull(event.getSide(), "Side should be null");
    assertNull(event.getType(), "Type should be null");
    assertEquals(0L, event.getPrice(), "Price should be 0");
    assertEquals(0L, event.getQuantity(), "Quantity should be 0");
    assertEquals(0L, event.getTimestampNanos(), "Timestamp should be 0");
    assertEquals(OrderEvent.EventState.EMPTY, event.getState(), "State should be EMPTY");
  }
}
