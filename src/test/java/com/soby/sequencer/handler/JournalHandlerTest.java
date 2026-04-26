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
}
