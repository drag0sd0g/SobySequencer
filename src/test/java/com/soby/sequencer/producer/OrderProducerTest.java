package com.soby.sequencer.producer;

import static org.junit.jupiter.api.Assertions.*;

import com.soby.sequencer.Sequencer;
import com.soby.sequencer.SequencerConfig;
import org.junit.jupiter.api.*;

public class OrderProducerTest {

  private Sequencer sequencer;
  private OrderProducer producer;

  @BeforeEach
  public void setUp() throws Exception {
    SequencerConfig config =
        new SequencerConfig.Builder()
            .ringBufferSize(256)
            .waitStrategy(SequencerConfig.WaitStrategyType.BLOCKING)
            .enableAffinity(false)
            .build();
    sequencer = new Sequencer(config);
    sequencer.start();
    producer = new OrderProducer(sequencer, 1000);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (sequencer != null) {
      sequencer.shutdown();
      Thread.sleep(100);
    }
  }

  @Test
  public void testPublishOrderReturnsLatency() {
    long latency = producer.publishOrder(1L);

    assertTrue(latency > 0, "Latency should be positive");
    assertTrue(latency < 1_000_000_000L, "Latency should be less than 1 second");
  }

  @Test
  public void testPublishBatchRecordsLatencies() {
    int count = 100;
    long[] latencies = producer.publishBatch(count);

    assertNotNull(latencies, "Latencies should not be null");
    assertEquals(count, producer.getLatencyCount(), "Latency count should match");
  }

  @Test
  public void testGetLatenciesReturnsRecordedValues() {
    producer.publishBatch(50);

    long[] latencies = producer.getLatencies();

    assertNotNull(latencies, "Latencies should not be null");
    assertTrue(latencies.length > 0, "Latencies should have values");
  }

  @Test
  public void testGetLatenciesWithMaxSize() {
    producer.publishBatch(100);

    long[] latencies = producer.getLatencies(10);

    assertNotNull(latencies, "Latencies should not be null");
    assertEquals(10, latencies.length, "Latencies array should be trimmed to maxSize");
  }

  @Test
  public void testResetClearsLatencyBuffer() {
    producer.publishBatch(50);
    assertEquals(50, producer.getLatencyCount(), "Should have 50 latencies before reset");

    producer.reset();

    assertEquals(0, producer.getLatencyCount(), "Latency count should be 0 after reset");
  }

  @Test
  public void testResetAllowsNewBatch() {
    producer.publishBatch(25);
    producer.reset();

    long[] newLatencies = producer.publishBatch(30);

    assertNotNull(newLatencies, "New latencies should not be null");
    assertEquals(30, producer.getLatencyCount(), "Should have 30 new latencies");
  }

  @Test
  public void testGetLatenciesCount() {
    assertEquals(0, producer.getLatencyCount(), "Should start with 0 latencies");
    producer.publishBatch(25);
    assertEquals(25, producer.getLatencyCount(), "Should have 25 latencies after batch");
  }
}
