package com.soby.sequencer.handler;

import com.lmax.disruptor.EventHandler;
import com.soby.sequencer.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that outputs the final event state.
 * In a real system this would publish execution reports back to clients via FIX protocol.
 * Currently logs the event state via SLF4J.
 */
public class OutputHandler implements EventHandler<OrderEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(OutputHandler.class);

    @Override
    public void onEvent(OrderEvent event, long sequenceNumber, boolean endOfBatch) {
        // This is where execution reports would be sent to clients via FIX
        // For now, we log the final state for demonstration purposes
        if (LOG.isTraceEnabled()) {
            LOG.trace("Event {}: order={}, symbol={}, side={}, type={}, price={}, qty={}, state={}",
                    sequenceNumber, event.getOrderId(), event.getSymbol(), event.getSide(),
                    event.getType(), event.getPrice(), event.getQuantity(), event.getState());
        }
        // No additional processing needed - event is already marked as PROCESSED
    }
}
