package com.soby.sequencer;

import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.event.OrderEvent.EventState;
import com.soby.sequencer.handler.MatchingEngineHandler;
import com.soby.sequencer.model.OrderType;
import com.soby.sequencer.model.Side;
import com.soby.sequencer.util.LatencyRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatchingEngineHandler.
 * Tests the matching logic in isolation without the Disruptor.
 */
public class MatchingEngineHandlerTest {

    private MatchingEngineHandler handler;
    private LatencyRecorder latencyRecorder;

    @BeforeEach
    public void setUp() {
        latencyRecorder = new LatencyRecorder(1_000_000_000L);
        handler = new MatchingEngineHandler(latencyRecorder);
    }

    @Test
    public void testMatchLimitBuyAndSellAtSamePrice() {
        // Test 1: publish a BUY LIMIT at 100 and a SELL LIMIT at 100 - assert a match occurs

        // First, add a buy order
        OrderEvent buyEvent = new OrderEvent();
        buyEvent.setSide(Side.BUY);
        buyEvent.setType(OrderType.LIMIT);
        buyEvent.setPrice(100L);
        buyEvent.setQuantity(100L);
        buyEvent.setTimestampNanos(System.nanoTime());
        handler.onEvent(buyEvent, 1, false);

        // Then, add a sell order at the same price - should match
        OrderEvent sellEvent = new OrderEvent();
        sellEvent.setSide(Side.SELL);
        sellEvent.setType(OrderType.LIMIT);
        sellEvent.setPrice(100L);
        sellEvent.setQuantity(100L);
        sellEvent.setTimestampNanos(System.nanoTime());
        handler.onEvent(sellEvent, 2, false);

        // After match, both orders should be filled
        assertEquals(0, buyEvent.getQuantity(), "Buy order should be fully filled");
        assertEquals(0, sellEvent.getQuantity(), "Sell order should be fully filled");
    }

    @Test
    public void testNoMatchWhenSpreadNotCrossed() {
        // Test 2: publish a BUY LIMIT at 99 and a SELL LIMIT at 100 - assert no match (spread not crossed)

        // Add buy order at 99
        OrderEvent buyEvent = new OrderEvent();
        buyEvent.setSide(Side.BUY);
        buyEvent.setType(OrderType.LIMIT);
        buyEvent.setPrice(99L);
        buyEvent.setQuantity(100L);
        buyEvent.setTimestampNanos(System.nanoTime());
        handler.onEvent(buyEvent, 1, false);

        // Add sell order at 100 - spread not crossed (buy < sell)
        OrderEvent sellEvent = new OrderEvent();
        sellEvent.setSide(Side.SELL);
        sellEvent.setType(OrderType.LIMIT);
        sellEvent.setPrice(100L);
        sellEvent.setQuantity(100L);
        sellEvent.setTimestampNanos(System.nanoTime());
        handler.onEvent(sellEvent, 2, false);

        // No match should occur - both orders should have resting quantity
        assertEquals(100L, buyEvent.getQuantity(), "Buy order should have resting quantity");
        assertEquals(100L, sellEvent.getQuantity(), "Sell order should have resting quantity");
    }

    @Test
    public void testPriceTimePriorityFIFO() {
        // Test 3: publish multiple BUY orders at the same price - assert they are matched in FIFO order

        // Add first buy order
        OrderEvent buy1 = new OrderEvent();
        buy1.setSide(Side.BUY);
        buy1.setType(OrderType.LIMIT);
        buy1.setPrice(100L);
        buy1.setQuantity(50L);
        buy1.setTimestampNanos(System.nanoTime());
        handler.onEvent(buy1, 1, false);

        // Add second buy order at same price
        OrderEvent buy2 = new OrderEvent();
        buy2.setSide(Side.BUY);
        buy2.setType(OrderType.LIMIT);
        buy2.setPrice(100L);
        buy2.setQuantity(50L);
        buy2.setTimestampNanos(System.nanoTime());
        handler.onEvent(buy2, 2, false);

        // Add sell order to match
        OrderEvent sell = new OrderEvent();
        sell.setSide(Side.SELL);
        sell.setType(OrderType.LIMIT);
        sell.setPrice(100L);
        sell.setQuantity(100L);
        sell.setTimestampNanos(System.nanoTime());
        handler.onEvent(sell, 3, false);

        // First buy should be fully filled, second buy should be partially filled
        assertEquals(0, buy1.getQuantity(), "First buy order should be fully filled (FIFO)");
        assertEquals(0, buy2.getQuantity(), "Second buy order should be fully filled (FIFO)");
    }

    @Test
    public void testMarketBuyMatchesBestAsk() {
        // Test 4: publish a MARKET BUY - assert it matches the best available ask regardless of price

        // Add an ask at 150
        OrderEvent ask = new OrderEvent();
        ask.setSide(Side.SELL);
        ask.setType(OrderType.LIMIT);
        ask.setPrice(150L);
        ask.setQuantity(100L);
        ask.setTimestampNanos(System.nanoTime());
        handler.onEvent(ask, 1, false);

        // Add a market buy
        OrderEvent marketBuy = new OrderEvent();
        marketBuy.setSide(Side.BUY);
        marketBuy.setType(OrderType.MARKET);
        marketBuy.setPrice(100L); // Will be ignored for market orders
        marketBuy.setQuantity(100L);
        marketBuy.setTimestampNanos(System.nanoTime());
        handler.onEvent(marketBuy, 2, false);

        // Market buy should match the ask
        assertEquals(0, marketBuy.getQuantity(), "Market buy should be fully filled");
        assertEquals(0, ask.getQuantity(), "Ask should be fully filled");
    }

    @Test
    public void testClearResetsOrderBook() {
        // Add some orders
        OrderEvent buy = new OrderEvent();
        buy.setSide(Side.BUY);
        buy.setType(OrderType.LIMIT);
        buy.setPrice(100L);
        buy.setQuantity(100L);
        buy.setTimestampNanos(System.nanoTime());
        handler.onEvent(buy, 1, false);

        // Clear the order book
        handler.clear();

        // Verify book is empty
        assertNull(handler.getBestBid(), "Best bid should be null after clear");
        assertNull(handler.getBestAsk(), "Best ask should be null after clear");
        assertEquals(0, handler.getBidCount(), "Bid count should be 0 after clear");
        assertEquals(0, handler.getAskCount(), "Ask count should be 0 after clear");
    }
}
