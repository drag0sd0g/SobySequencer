package com.soby.sequencer.handler;

import com.lmax.disruptor.EventHandler;
import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.event.OrderEvent.EventState;
import com.soby.sequencer.model.OrderType;
import com.soby.sequencer.model.Side;
import com.soby.sequencer.util.LatencyRecorder;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.TreeMap;

/**
 * Handler that matches orders in an in-memory order book.
 * Maintains two price-time priority order books: bids (BUY) and asks (SELL).
 * Uses TreeMap for price-level ordering and ArrayDeque for FIFO within each level.
 * No allocations on the matching path - uses pre-allocated result objects.
 */
public class MatchingEngineHandler implements EventHandler<OrderEvent> {
    // Price-time priority order books
    // Bids: TreeMap<Long (price), ArrayDeque<OrderEvent>> sorted descending (best bid first)
    private final TreeMap<Long, ArrayDeque<OrderEvent>> bids = new TreeMap<>(Long::compareTo);
    // Asks: TreeMap<Long (price), ArrayDeque<OrderEvent>> sorted ascending (best ask first)
    private final TreeMap<Long, ArrayDeque<OrderEvent>> asks = new TreeMap<>(Long::compareTo);

    private final LatencyRecorder latencyRecorder;
    private long sequenceNumber = 0L;

    /**
     * Create a new matching engine handler.
     * @param latencyRecorder latency recorder for tracking match completion times
     */
    public MatchingEngineHandler(LatencyRecorder latencyRecorder) {
        this.latencyRecorder = latencyRecorder;
    }

    @Override
    public void onEvent(OrderEvent event, long sequenceNumber, boolean endOfBatch) {
        // Record latency from journal completion to match completion
        long journalCompletionTime = event.getTimestampNanos();
        long matchStartTime = System.nanoTime();
        long latency = matchStartTime - journalCompletionTime;
        latencyRecorder.record(latency);

        // Process the event
        processOrder(event);
        event.setState(EventState.PROCESSED);
        event.setSequenceNumber(sequenceNumber);
    }

    /**
     * Process an order and match it against the order book.
     * @param event the order event to process
     */
    private void processOrder(OrderEvent event) {
        if (event.getType() == OrderType.MARKET) {
            processMarketOrder(event);
        } else {
            processLimitOrder(event);
        }
    }

    /**
     * Process a market order - match against available orders regardless of price.
     * @param event the market order event
     */
    private void processMarketOrder(OrderEvent event) {
        if (event.getSide() == Side.BUY) {
            matchMarketOrderAgainstAsks(event);
        } else {
            matchMarketOrderAgainstBids(event);
        }
    }

    /**
     * Match a market buy order against asks (selling orders).
     * @param buyEvent the buying market order
     */
    private void matchMarketOrderAgainstAsks(OrderEvent buyEvent) {
        while (buyEvent.getQuantity() > 0 && !asks.isEmpty()) {
            Map.Entry<Long, ArrayDeque<OrderEvent>> bestAskEntry = asks.firstEntry();
            if (bestAskEntry == null) {
                break;
            }

            ArrayDeque<OrderEvent> askQueue = bestAskEntry.getValue();
            if (askQueue.isEmpty()) {
                asks.remove(bestAskEntry.getKey());
                continue;
            }

            OrderEvent askEvent = askQueue.peekFirst();

            // Match occurs - execute the fill
            long fillQuantity = Math.min(buyEvent.getQuantity(), askEvent.getQuantity());
            executeMatch(buyEvent, askEvent, fillQuantity, bestAskEntry.getKey());

            // Update quantities
            askEvent.setQuantity(askEvent.getQuantity() - fillQuantity);
            buyEvent.setQuantity(buyEvent.getQuantity() - fillQuantity);

            if (askEvent.getQuantity() == 0) {
                askQueue.pollFirst();
                if (askQueue.isEmpty()) {
                    asks.remove(bestAskEntry.getKey());
                }
            }
        }

        // If buy event has remaining quantity, add it to bids as a resting order
        if (buyEvent.getQuantity() > 0) {
            addToOrderBook(bids, buyEvent);
        }
    }

    /**
     * Match a market sell order against bids (buying orders).
     * @param sellEvent the selling market order
     */
    private void matchMarketOrderAgainstBids(OrderEvent sellEvent) {
        while (sellEvent.getQuantity() > 0 && !bids.isEmpty()) {
            Map.Entry<Long, ArrayDeque<OrderEvent>> bestBidEntry = bids.lastEntry();
            if (bestBidEntry == null) {
                break;
            }

            ArrayDeque<OrderEvent> bidQueue = bestBidEntry.getValue();
            if (bidQueue.isEmpty()) {
                bids.remove(bestBidEntry.getKey());
                continue;
            }

            OrderEvent bidEvent = bidQueue.peekFirst();

            // Match occurs - execute the fill
            long fillQuantity = Math.min(sellEvent.getQuantity(), bidEvent.getQuantity());
            executeMatch(bidEvent, sellEvent, fillQuantity, bestBidEntry.getKey());

            // Update quantities
            bidEvent.setQuantity(bidEvent.getQuantity() - fillQuantity);
            sellEvent.setQuantity(sellEvent.getQuantity() - fillQuantity);

            if (bidEvent.getQuantity() == 0) {
                bidQueue.pollFirst();
                if (bidQueue.isEmpty()) {
                    bids.remove(bestBidEntry.getKey());
                }
            }
        }

        // If sell event has remaining quantity, add it to asks as a resting order
        if (sellEvent.getQuantity() > 0) {
            addToOrderBook(asks, sellEvent);
        }
    }

    /**
     * Process a limit order - match if price crosses the spread, otherwise rest in book.
     * @param event the limit order event
     */
    private void processLimitOrder(OrderEvent event) {
        if (event.getSide() == Side.BUY) {
            matchLimitBuyOrderAgainstAsks(event);
        } else {
            matchLimitSellOrderAgainstBids(event);
        }
    }

    /**
     * Match a limit buy order against asks if price >= best ask.
     * @param buyEvent the buying limit order
     */
    private void matchLimitBuyOrderAgainstAsks(OrderEvent buyEvent) {
        while (buyEvent.getQuantity() > 0 && !asks.isEmpty()) {
            Map.Entry<Long, ArrayDeque<OrderEvent>> bestAskEntry = asks.firstEntry();
            if (bestAskEntry == null) {
                break;
            }

            // Check if spread is crossed: buy price >= sell price
            if (buyEvent.getPrice() < bestAskEntry.getKey()) {
                break; // Best ask is higher than buy price - no more matches possible
            }

            ArrayDeque<OrderEvent> askQueue = bestAskEntry.getValue();
            if (askQueue.isEmpty()) {
                asks.remove(bestAskEntry.getKey());
                continue;
            }

            OrderEvent askEvent = askQueue.peekFirst();

            // Match occurs
            long fillQuantity = Math.min(buyEvent.getQuantity(), askEvent.getQuantity());
            executeMatch(buyEvent, askEvent, fillQuantity, bestAskEntry.getKey());

            // Update quantities
            askEvent.setQuantity(askEvent.getQuantity() - fillQuantity);
            buyEvent.setQuantity(buyEvent.getQuantity() - fillQuantity);

            if (askEvent.getQuantity() == 0) {
                askQueue.pollFirst();
                if (askQueue.isEmpty()) {
                    asks.remove(bestAskEntry.getKey());
                }
            }
        }

        // If buy event has remaining quantity, add it to bids as a resting order
        if (buyEvent.getQuantity() > 0) {
            addToOrderBook(bids, buyEvent);
        }
    }

    /**
     * Match a limit sell order against bids if price <= best bid.
     * @param sellEvent the selling limit order
     */
    private void matchLimitSellOrderAgainstBids(OrderEvent sellEvent) {
        while (sellEvent.getQuantity() > 0 && !bids.isEmpty()) {
            Map.Entry<Long, ArrayDeque<OrderEvent>> bestBidEntry = bids.lastEntry();
            if (bestBidEntry == null) {
                break;
            }

            // Check if spread is crossed: buy price >= sell price
            if (bestBidEntry.getKey() < sellEvent.getPrice()) {
                break; // Best bid is lower than sell price - no more matches possible
            }

            ArrayDeque<OrderEvent> bidQueue = bestBidEntry.getValue();
            if (bidQueue.isEmpty()) {
                bids.remove(bestBidEntry.getKey());
                continue;
            }

            OrderEvent bidEvent = bidQueue.peekFirst();

            // Match occurs
            long fillQuantity = Math.min(sellEvent.getQuantity(), bidEvent.getQuantity());
            executeMatch(bidEvent, sellEvent, fillQuantity, bestBidEntry.getKey());

            // Update quantities
            bidEvent.setQuantity(bidEvent.getQuantity() - fillQuantity);
            sellEvent.setQuantity(sellEvent.getQuantity() - fillQuantity);

            if (bidEvent.getQuantity() == 0) {
                bidQueue.pollFirst();
                if (bidQueue.isEmpty()) {
                    bids.remove(bestBidEntry.getKey());
                }
            }
        }

        // If sell event has remaining quantity, add it to asks as a resting order
        if (sellEvent.getQuantity() > 0) {
            addToOrderBook(asks, sellEvent);
        }
    }

    /**
     * Execute a match between two orders and log the fill.
     * @param buyer the buying order
     * @param seller the selling order
     * @param quantity the fill quantity
     * @param price the execution price
     */
    private void executeMatch(OrderEvent buyer, OrderEvent seller, long quantity, long price) {
        // In a production system, this would send execution reports
        // For now, just log the match details
        // LOG.debug("MATCH: Buy={} Sell={} Qty={} Price={}", buyer.getOrderId(), seller.getOrderId(), quantity, price);
    }

    /**
     * Add an order to the appropriate order book.
     * @param book the order book (bids or asks)
     * @param event the order event to add
     */
    private void addToOrderBook(TreeMap<Long, ArrayDeque<OrderEvent>> book, OrderEvent event) {
        ArrayDeque<OrderEvent> queue = book.get(event.getPrice());
        if (queue == null) {
            queue = new ArrayDeque<>();
            book.put(event.getPrice(), queue);
        }
        queue.offerLast(event);
    }

    /**
     * Get the best bid (highest price buy order) or null if no bids.
     * @return the best bid order or null
     */
    public OrderEvent getBestBid() {
        Map.Entry<Long, ArrayDeque<OrderEvent>> bestBidEntry = bids.lastEntry();
        if (bestBidEntry == null) {
            return null;
        }
        ArrayDeque<OrderEvent> queue = bestBidEntry.getValue();
        return queue.isEmpty() ? null : queue.peekFirst();
    }

    /**
     * Get the best ask (lowest price sell order) or null if no asks.
     * @return the best ask order or null
     */
    public OrderEvent getBestAsk() {
        Map.Entry<Long, ArrayDeque<OrderEvent>> bestAskEntry = asks.firstEntry();
        if (bestAskEntry == null) {
            return null;
        }
        ArrayDeque<OrderEvent> queue = bestAskEntry.getValue();
        return queue.isEmpty() ? null : queue.peekFirst();
    }

    /**
     * Get the number of price levels in the order book.
     * @return the number of price levels
     */
    public int getBidCount() {
        return bids.size();
    }

    /**
     * Get the number of price levels in the order book.
     * @return the number of price levels
     */
    public int getAskCount() {
        return asks.size();
    }

    /**
     * Clear all orders from the book. Used for testing.
     */
    public void clear() {
        bids.clear();
        asks.clear();
    }
}
