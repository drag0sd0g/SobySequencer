package com.soby.sequencer.event;

import com.lmax.disruptor.EventFactory;

/**
 * Factory for creating pre-allocated OrderEvent instances for the ring buffer. This eliminates GC
 * pressure by reusing event instances instead of creating new ones.
 */
public class OrderEventFactory implements EventFactory<OrderEvent> {
  @Override
  public OrderEvent newInstance() {
    return new OrderEvent();
  }
}
