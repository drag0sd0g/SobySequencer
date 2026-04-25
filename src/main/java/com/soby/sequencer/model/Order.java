package com.soby.sequencer.model;

/**
 * Represents an order to be processed by the sequencer.
 * Immutable value object passed from producer to the ring buffer.
 */
public class Order {
    private final long orderId;
    private final String symbol;
    private final Side side;
    private final OrderType type;
    private final long price;
    private final long quantity;

    public Order(long orderId, String symbol, Side side, OrderType type, long price, long quantity) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Side getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }
}
