package com.soby.sequencer.model;

/**
 * Represents an order to be processed by the sequencer.
 * Immutable value object passed from producer to the ring buffer.
 */
public record Order(long orderId, String symbol, Side side, OrderType type, long price, long quantity) {
}
