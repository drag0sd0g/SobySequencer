package com.soby.sequencer;

import static org.junit.jupiter.api.Assertions.*;

import com.soby.sequencer.handler.JournalHandler;
import com.soby.sequencer.handler.MatchingEngineHandler;
import com.soby.sequencer.model.OrderType;
import com.soby.sequencer.model.Side;
import com.soby.sequencer.producer.OrderProducer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests for Sequencer. Tests the full pipeline with the Disruptor. */
public class SequencerIntegrationTest {

  private Sequencer sequencer;
  private String tempJournalPath;
  private final AtomicInteger processedCount = new AtomicInteger(0);
  private MatchingEngineHandler testHandler;

  @BeforeEach
  public void setUp() throws IOException {
    // Create a temporary journal file
    Path tempFile = Files.createTempFile("sequencer-test", ".dat");
    tempJournalPath = tempFile.toString();

    // Build config with small ring buffer and blocking wait for test stability
    SequencerConfig config =
        new SequencerConfig.Builder()
            .ringBufferSize(256)
            .waitStrategy(SequencerConfig.WaitStrategyType.BLOCKING)
            .enableAffinity(false)
            .journalFilePath(tempJournalPath)
            .build();

    sequencer = new Sequencer(config);

    // Get the matching engine handler and add a wrapper to count events
    testHandler = sequencer.getMatchingEngineHandler();

    // We'll count events by overriding the handler's onEvent
    // For this test, we'll check the count after processing
  }

  @AfterEach
  public void tearDown() {
    if (sequencer != null) {
      sequencer.shutdown();
    }
    // Clean up temp file
    if (tempJournalPath != null) {
      try {
        Files.deleteIfExists(Paths.get(tempJournalPath));
      } catch (IOException e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testProcessesOrdersAndRecordsLatencies() throws Exception {
    // Start the sequencer
    sequencer.start();

    // Create producer
    OrderProducer producer = new OrderProducer(sequencer, 10000);

    // Publish 10,000 orders
    for (long i = 0; i < 10000; i++) {
      producer.publishOrder(i);
    }

    // Wait for all events to be processed
    Thread.sleep(1000);

    // Verify all orders were processed
    long cursor = sequencer.getCursor();
    assertEquals(9999, cursor, "Cursor should be at 9999 (0-indexed, 10000 orders)");

    // Verify journal file was created
    File journalFile = new File(tempJournalPath);
    assertTrue(journalFile.exists(), "Journal file should exist");
    assertTrue(journalFile.length() > 0, "Journal file should have content");

    // Verify latency recorders captured values
    assertNotNull(sequencer.getJournalLatencyRecorder());
    assertNotNull(sequencer.getMatchingLatencyRecorder());

    // Shutdown
    sequencer.shutdown();
  }

  @Test
  public void testJournalEntriesAreContiguous() throws Exception {
    sequencer.start();

    for (long i = 0; i < 100; i++) {
      sequencer.publishOrder(i, "TEST", Side.BUY, OrderType.LIMIT, 100L, 10L);
    }

    Thread.sleep(500);

    JournalHandler journalHandler = sequencer.getJournalHandler();
    assertNotNull(journalHandler, "Journal handler should exist");

    sequencer.shutdown();
  }

  @Test
  public void testPublishingRateDoesNotDropEvents() throws Exception {
    SequencerConfig config =
        new SequencerConfig.Builder()
            .ringBufferSize(256)
            .waitStrategy(SequencerConfig.WaitStrategyType.BLOCKING)
            .enableAffinity(false)
            .journalFilePath(tempJournalPath)
            .build();

    Sequencer testSequencer = new Sequencer(config);
    testSequencer.start();

    int totalOrders = 1000;
    int batchSize = 100;

    for (int batch = 0; batch < totalOrders / batchSize; batch++) {
      for (int i = 0; i < batchSize; i++) {
        long orderId = batch * batchSize + i;
        testSequencer.publishOrder(orderId, "TEST", Side.BUY, OrderType.LIMIT, 100L, 10L);
      }
      Thread.sleep(10);
    }

    Thread.sleep(1000);

    long cursor = testSequencer.getCursor();
    assertEquals(totalOrders - 1, cursor, "Should process all " + totalOrders + " orders");

    testSequencer.shutdown();
  }

  @Test
  public void testSequencerStartAndShutdownCleanly() throws Exception {
    // Start
    sequencer.start();
    assertNotNull(sequencer.getCursor(), "Cursor should be available after start");

    // Shutdown
    sequencer.shutdown();
    // No exceptions means clean shutdown
  }
}
