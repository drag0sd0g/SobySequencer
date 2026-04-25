package com.soby.sequencer;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.event.OrderEventFactory;
import com.soby.sequencer.handler.MatchingEngineHandler;
import com.soby.sequencer.handler.OutputHandler;
import com.soby.sequencer.handler.JournalHandler;
import com.soby.sequencer.model.Order;
import com.soby.sequencer.util.AffinitySupport;
import com.soby.sequencer.util.LatencyRecorder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Core sequencer class that owns and manages the Disruptor instance.
 * Wires together the handler pipeline: JournalHandler -> MatchingEngineHandler -> OutputHandler
 * Uses a single producer (single writer principle) for zero-contention writes.
 */
public class Sequencer {
    private Disruptor<OrderEvent> disruptor;
    private final JournalHandler journalHandler;
    private final MatchingEngineHandler matchingEngineHandler;
    private final OutputHandler outputHandler;
    private ExecutorService executor;
    private final SequencerConfig config;

    // Latency recorders
    private final LatencyRecorder journalLatencyRecorder;
    private final LatencyRecorder matchingLatencyRecorder;
    private final LatencyRecorder endToEndLatencyRecorder;

    /**
     * Create a new Sequencer with the given configuration.
     * @param config the sequencer configuration
     * @throws IOException if journal file cannot be created
     */
    public Sequencer(SequencerConfig config) throws IOException {
        this.config = config;

        // Create latency recorders - track up to 1 second (1 billion nanoseconds)
        this.journalLatencyRecorder = new LatencyRecorder(1_000_000_000L);
        this.matchingLatencyRecorder = new LatencyRecorder(1_000_000_000L);
        this.endToEndLatencyRecorder = new LatencyRecorder(1_000_000_000L);

        // Create handlers
        this.journalHandler = new JournalHandler(config.getJournalFilePath(), journalLatencyRecorder);
        this.matchingEngineHandler = new MatchingEngineHandler(matchingLatencyRecorder);
        this.outputHandler = new OutputHandler();

        // Create executor
        this.executor = Executors.newCachedThreadPool();

        // Create disruptor - Disruptor 3.4.4 constructor
        this.disruptor = new Disruptor<>(new OrderEventFactory(), config.getRingBufferSize(), executor, ProducerType.SINGLE, getWaitStrategy(config.getWaitStrategy()));

        // Set up the handler pipeline
        disruptor.handleEventsWith(journalHandler, new ReplicaHandler())
                .then(matchingEngineHandler)
                .then(outputHandler);
    }

    /**
     * Get the wait strategy based on configuration.
     * @param type the wait strategy type
     * @return the Disruptor WaitStrategy
     */
    private WaitStrategy getWaitStrategy(SequencerConfig.WaitStrategyType type) {
        switch (type) {
            case BUSY_SPIN:
                return new com.lmax.disruptor.BusySpinWaitStrategy();
            case YIELDING:
                return new com.lmax.disruptor.YieldingWaitStrategy();
            case SLEEPING:
                return new com.lmax.disruptor.SleepingWaitStrategy();
            case BLOCKING:
                return new com.lmax.disruptor.BlockingWaitStrategy();
            default:
                return new com.lmax.disruptor.BusySpinWaitStrategy();
        }
    }

    /**
     * Start the sequencer and begin processing events.
     * Optionally pins the main thread to a CPU core for lowest latency.
     * @throws IOException if journal cannot be initialized
     */
    public void start() throws IOException {
        if (config.isEnableAffinity()) {
            AffinitySupport.pinCurrentThreadToCore(config.getSequencerCpuCore());
        }
        disruptor.start();
    }

    /**
     * Publish an order to the sequencer.
     * Zero-allocation on the publish path - uses a pre-allocated event slot.
     * @param order the order to publish
     */
    public void publishOrder(Order order) {
        RingBuffer<OrderEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            // Populate the event slot
            event.setSequenceNumber(sequence);
            event.setOrderId(order.orderId());
            event.setSymbol(order.symbol());
            event.setSide(order.side());
            event.setType(order.type());
            event.setPrice(order.price());
            event.setQuantity(order.quantity());
            event.setTimestampNanos(System.nanoTime());
            event.setState(OrderEvent.EventState.PUBLISHED);
        } finally {
            // release barrier ensures event data is visible before publishing sequence
            ringBuffer.publish(sequence);
        }
    }

    /**
     * Shutdown the sequencer gracefully.
     * Shuts down the disruptor and journal handler.
     */
    public void shutdown() {
        try {
            disruptor.shutdown(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore shutdown exceptions
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            journalHandler.close();
        } catch (IOException e) {
            // Ignore close exceptions during shutdown
        }
    }

    /**
     * Get the journal handler for testing access.
     * @return the journal handler
     */
    public JournalHandler getJournalHandler() {
        return journalHandler;
    }

    /**
     * Get the matching engine handler for testing access.
     * @return the matching engine handler
     */
    public MatchingEngineHandler getMatchingEngineHandler() {
        return matchingEngineHandler;
    }

    /**
     * Get the sequence number of the next event to be published.
     * @return next sequence number
     */
    public long getNextSequence() {
        return disruptor.getRingBuffer().next();
    }

    /**
     * Get the current ring buffer cursor (last published sequence).
     * @return current cursor
     */
    public long getCursor() {
        return disruptor.getRingBuffer().getCursor();
    }

    /**
     * Get latency recorder for the journal handler.
     * @return journal latency recorder
     */
    public LatencyRecorder getJournalLatencyRecorder() {
        return journalLatencyRecorder;
    }

    /**
     * Get latency recorder for the matching engine handler.
     * @return matching latency recorder
     */
    public LatencyRecorder getMatchingLatencyRecorder() {
        return matchingLatencyRecorder;
    }

    /**
     * Get latency recorder for end-to-end latency tracking.
     * @return end-to-end latency recorder
     */
    public LatencyRecorder getEndToEndLatencyRecorder() {
        return endToEndLatencyRecorder;
    }

    /**
     * Stub replica handler for parallel processing in the diamond dependency graph.
     * In a production system this would write to a secondary journal or send to a replica.
     */
    private static class ReplicaHandler implements com.lmax.disruptor.EventHandler<OrderEvent> {
        @Override
        public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
            // This handler runs in parallel with JournalHandler
            // In production: write to replica journal or send over network
            // For now: just mark the event as processed
        }
    }
}
