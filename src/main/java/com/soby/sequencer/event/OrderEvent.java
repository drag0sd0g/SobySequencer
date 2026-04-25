package com.soby.sequencer.event;

import com.soby.sequencer.model.OrderType;
import com.soby.sequencer.model.Side;

/**
 * Pre-allocated, mutable event object that lives in the ring buffer.
 * All fields are reset via setters to avoid allocations during normal operation.
 */
public class OrderEvent {
    public static enum EventState {
        EMPTY((byte) 0),
        PUBLISHED((byte) 1),
        PROCESSED((byte) 2);

        private final byte value;

        EventState(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    private long sequenceNumber;
    private long orderId;
    private String symbol;
    private Side side;
    private OrderType type;
    private long price;
    private long quantity;
    private long timestampNanos;
    private EventState state;

    public void reset() {
        sequenceNumber = 0L;
        orderId = 0L;
        symbol = null;
        side = null;
        type = null;
        price = 0L;
        quantity = 0L;
        timestampNanos = 0L;
        state = EventState.EMPTY;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public void setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
    }

    public EventState getState() {
        return state;
    }

    public void setState(EventState state) {
        this.state = state;
    }
}
