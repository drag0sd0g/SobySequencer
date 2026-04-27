package com.soby.sequencer.handler;

import com.lmax.disruptor.EventHandler;
import com.soby.sequencer.event.OrderEvent;
import com.soby.sequencer.util.LatencyRecorder;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that writes events to a memory-mapped file journal for durability. Uses a pre-allocated
 * 64MB mapped file for zero-copy journaling with fixed-width binary format. Format: sequenceNumber
 * (8 bytes) + orderId (8 bytes) + price (8 bytes) + quantity (8 bytes) + side (1 byte) + type (1
 * byte) = 34 bytes per entry
 */
public class JournalHandler implements EventHandler<OrderEvent>, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(JournalHandler.class);
  private static final int ENTRY_SIZE =
      34; // sequenceNumber (8) + orderId (8) + price (8) + quantity (8) + side (1) + type (1)
  private static final long FILE_SIZE = 64L * 1024 * 1024; // 64MB
  private static final int MAX_EVENTS = (int) (FILE_SIZE / ENTRY_SIZE);

  private final File journalFile;
  private final FileChannel fileChannel;
  private final MappedByteBuffer mappedBuffer;
  private final LatencyRecorder latencyRecorder;
  private long position = 0L;

  /**
   * Create a new journal handler.
   *
   * @param journalFilePath path to the journal file
   * @param latencyRecorder latency recorder for tracking journal write times
   * @throws IOException if file cannot be created or mapped
   */
  public JournalHandler(String journalFilePath, LatencyRecorder latencyRecorder)
      throws IOException {
    this.journalFile = new File(journalFilePath);
    File parentDir = journalFile.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }

    // Pre-allocate the file and map it to memory
    this.fileChannel = new RandomAccessFile(journalFile, "rw").getChannel();
    this.mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, FILE_SIZE);
    this.latencyRecorder = latencyRecorder;
  }

  /**
   * Write an event to the journal. Records latency from event timestamp to journal write
   * completion.
   *
   * @param event the event to journal
   * @return the journal position after writing
   */
  public long journal(OrderEvent event) {
    var entryOffset = (int) (position % MAX_EVENTS) * ENTRY_SIZE;
    mappedBuffer.putLong(entryOffset, event.getSequenceNumber());
    mappedBuffer.putLong(entryOffset + 8, event.getOrderId());
    mappedBuffer.putLong(entryOffset + 16, event.getPrice());
    mappedBuffer.putLong(entryOffset + 24, event.getQuantity());
    mappedBuffer.put(entryOffset + 32, event.getSide().getValue());
    mappedBuffer.put(entryOffset + 33, event.getType().getValue());

    var journalCompletionTime = System.nanoTime();
    position++;

    // Record latency from event original timestamp to journal completion
    var latency = journalCompletionTime - event.getTimestampNanos();
    latencyRecorder.record(latency);

    return position;
  }

  /**
   * Get the current journal position (number of entries written).
   *
   * @return journal position
   */
  public long getPosition() {
    return position;
  }

  /**
   * EventHandler implementation - processes event from the ring buffer.
   *
   * @param event the event to process
   * @param sequence the sequence number
   * @param endOfBatch true if this is the last event in the batch
   */
  @Override
  public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
    journal(event);
  }

  /**
   * Close the journal and ensure all data is flushed to disk.
   *
   * @throws IOException if close fails
   */
  @Override
  public void close() throws IOException {
    mappedBuffer.force();
    fileChannel.close();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Journal closed. Entries written: {}", position);
    }
  }

  /**
   * Get the journal file path.
   *
   * @return journal file path
   */
  String getJournalFilePath() {
    return journalFile.getAbsolutePath();
  }

  /**
   * Get the journal file channel for testing.
   *
   * @return file channel
   */
  FileChannel getFileChannel() {
    return fileChannel;
  }

  /**
   * Get the latency recorder for testing.
   *
   * @return latency recorder
   */
  LatencyRecorder getLatencyRecorder() {
    return latencyRecorder;
  }
}
