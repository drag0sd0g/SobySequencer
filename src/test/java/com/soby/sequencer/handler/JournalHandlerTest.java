package com.soby.sequencer.handler;

import static org.junit.jupiter.api.Assertions.*;

import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.util.LatencyRecorder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JournalHandlerTest {

  private String tempJournalPath;
  private LatencyRecorder latencyRecorder;

  @BeforeEach
  public void setUp() throws IOException {
    Path tempFile = Files.createTempFile("journal-test", ".dat");
    tempJournalPath = tempFile.toString();
    latencyRecorder = new LatencyRecorder(1_000_000_000L);
  }

  @AfterEach
  public void tearDown() {
    if (tempJournalPath != null) {
      try {
        Files.deleteIfExists(Path.of(tempJournalPath));
      } catch (IOException e) {
        // Ignore cleanup errors
      }
    }
  }

  @Test
  public void testJournalHandlerWritesToMappedBuffer() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    OrderEvent event = new OrderEvent();
    event.setSequenceNumber(1L);
    event.setOrderId(100L);
    event.setPrice(15000L);
    event.setQuantity(50L);
    event.setSide(com.soby.sequencer.model.Side.BUY);
    event.setType(com.soby.sequencer.model.OrderType.LIMIT);
    event.setTimestampNanos(System.nanoTime());
    event.setState(OrderEvent.EventState.PUBLISHED);

    long position = handler.journal(event);

    assertEquals(1, position, "Position should be 1 after first write");
    assertNotNull(handler.getFileChannel(), "File channel should be available");
  }

  @Test
  public void testJournalWithParametersWritesToMappedBuffer() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    long position =
        handler.journal(
            2L, // sequenceNumber
            200L, // orderId
            25000L, // price
            75L, // quantity
            (byte) 1, // side (SELL)
            (byte) 0 // type (MARKET)
            );

    assertEquals(1, position, "Position should be 1 after first write");
  }

  @Test
  public void testGetPositionReturnsWrittenCount() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    for (int i = 0; i < 5; i++) {
      long sequence = i + 1L;
      handler.journal(sequence, 100L + i, 100L + i, 10L + i, (byte) 0, (byte) 1);
    }

    assertEquals(5, handler.getPosition(), "Position should be 5 after 5 writes");
  }

  @Test
  public void testJournalFilePathReturnsAbsolute_path() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    String path = handler.getJournalFilePath();
    File file = new File(path);

    assertTrue(file.isAbsolute(), "Path should be absolute");
    assertEquals(new File(tempJournalPath).getName(), file.getName(), "Filenames should match");
  }

  @Test
  public void testFileChannelIsNotNull() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    assertNotNull(handler.getFileChannel(), "File channel should not be null");
  }

  @Test
  public void testMultipleJournalCallsAdvancePosition() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    long pos1 = handler.journal(1L, 100L, 100L, 10L, (byte) 0, (byte) 1);
    long pos2 = handler.journal(2L, 200L, 200L, 20L, (byte) 1, (byte) 0);
    long pos3 = handler.journal(3L, 300L, 300L, 30L, (byte) 0, (byte) 1);

    assertEquals(1, pos1, "First position should be 1");
    assertEquals(2, pos2, "Second position should be 2");
    assertEquals(3, pos3, "Third position should be 3");
    assertEquals(3, handler.getPosition(), "Final position should be 3");
  }

  @Test
  public void testJournalOnEventIntegration() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    OrderEvent event = new OrderEvent();
    event.setSequenceNumber(0L);
    event.setOrderId(1000L);
    event.setPrice(50000L);
    event.setQuantity(100L);
    event.setSide(com.soby.sequencer.model.Side.BUY);
    event.setType(com.soby.sequencer.model.OrderType.LIMIT);
    event.setTimestampNanos(System.nanoTime());

    handler.onEvent(event, 1L, false);

    assertEquals(1L, handler.getPosition(), "Position should be advanced by onEvent");
  }

  @Test
  public void testJournalHandlerCloseFlushesData() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    handler.journal(1L, 100L, 100L, 10L, (byte) 0, (byte) 1);

    handler.close();

    File file = new File(tempJournalPath);
    assertTrue(file.exists(), "Journal file should exist after close");
    assertTrue(file.length() > 0, "Journal file should have content after close");
  }

  @Test
  public void testJournalHandlerLatencyRecording() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    OrderEvent event = new OrderEvent();
    event.setSequenceNumber(1L);
    event.setOrderId(100L);
    event.setPrice(100L);
    event.setQuantity(10L);
    event.setSide(com.soby.sequencer.model.Side.BUY);
    event.setType(com.soby.sequencer.model.OrderType.LIMIT);
    event.setTimestampNanos(System.nanoTime() - 1000000L);

    handler.journal(event);

    // Verify latency was recorded by checking printReport doesn't throw
    assertDoesNotThrow(
        () -> handler.getLatencyRecorder().printReport("Test"), "Should record latency");
  }

  @Test
  public void testJournalHandlerHandlesRingBufferWraparound() throws Exception {
    JournalHandler handler = new JournalHandler(tempJournalPath, latencyRecorder);

    int totalEvents = 2000;
    for (int i = 0; i < totalEvents; i++) {
      handler.journal(i + 1L, 100L + i, 100L + i, 10L + i, (byte) (i % 2), (byte) (i % 2));
    }

    assertEquals(totalEvents, handler.getPosition(), "Position should equal total events");
  }
}
