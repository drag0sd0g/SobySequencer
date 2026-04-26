package com.soby.sequencer.producer;

import com.soby.sequencer.Sequencer;
import com.soby.sequencer.model.OrderType;
import com.soby.sequencer.model.Side;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Produces synthetic orders for testing the sequencer. Uses ThreadLocalRandom for lock-free random
 * number generation on the hot path. Records end-to-end latency for benchmarking.
 */
public class OrderProducer {
  private static final String[] SYMBOLS = {
    "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "META", "NVDA", "AMD"
  };
  private static final long PRICE_MIN = 10000L; // 100.00 in pence/ticks
  private static final long PRICE_MAX = 50000L; // 500.00 in pence/ticks
  private static final long QUANTITY_MIN = 10L;
  private static final long QUANTITY_MAX = 1000L;

  private final Sequencer sequencer;
  private final long[] endToEndLatencies;
  private int latencyIndex = 0;

  /**
   * Create a new order producer.
   *
   * @param sequencer the sequencer to publish orders to
   * @param capacity the number of latency samples to buffer
   */
  public OrderProducer(Sequencer sequencer, int capacity) {
    this.sequencer = sequencer;
    this.endToEndLatencies = new long[capacity];
  }

  public long publishOrder(long orderId) {
    var startTime = System.nanoTime();
    var symbol = SYMBOLS[ThreadLocalRandom.current().nextInt(SYMBOLS.length)];
    var side = ThreadLocalRandom.current().nextBoolean() ? Side.BUY : Side.SELL;
    var type = ThreadLocalRandom.current().nextBoolean() ? OrderType.LIMIT : OrderType.MARKET;
    var price = PRICE_MIN + ThreadLocalRandom.current().nextLong(PRICE_MAX - PRICE_MIN + 1);
    var quantity =
        QUANTITY_MIN + ThreadLocalRandom.current().nextLong(QUANTITY_MAX - QUANTITY_MIN + 1);
    sequencer.publishOrder(orderId, symbol, side, type, price, quantity);
    return System.nanoTime() - startTime;
  }

  /**
   * Publish a batch of orders and record their latencies.
   *
   * @param count the number of orders to publish
   * @return the array of latencies for each published order
   */
  public long[] publishBatch(int count) {
    for (int i = 0; i < count; i++) {
      long latency = publishOrder(i);
      if (latencyIndex < endToEndLatencies.length) {
        endToEndLatencies[latencyIndex++] = latency;
      }
    }
    return endToEndLatencies;
  }

  /**
   * Get the recorded latencies from the last batch.
   *
   * @return array of latencies
   */
  public long[] getLatencies() {
    return endToEndLatencies;
  }

  /**
   * Get the index of the last recorded latency.
   *
   * @return number of latencies recorded
   */
  public int getLatencyCount() {
    return latencyIndex;
  }

  /**
   * Get the recorded latencies.
   *
   * @param maxSize maximum number of latencies to return
   * @return array of latencies
   */
  public long[] getLatencies(int maxSize) {
    int size = Math.min(latencyIndex, maxSize);
    long[] result = new long[size];
    System.arraycopy(endToEndLatencies, 0, result, 0, size);
    return result;
  }

  /** Reset the latency buffer for a new benchmark run. */
  public void reset() {
    latencyIndex = 0;
  }
}
