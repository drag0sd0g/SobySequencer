package com.soby.sequencer;

import static org.junit.jupiter.api.Assertions.*;

import com.soby.sequencer.util.LatencyRecorder;
import org.junit.jupiter.api.*;

public class SequencerTest {

  private Sequencer sequencer;

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
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (sequencer != null) {
      sequencer.shutdown();
    }
  }

  @Test
  public void testGetNextSequenceReturnsNextSequence() throws Exception {
    long next1 = sequencer.getNextSequence();
    long next2 = sequencer.getNextSequence();
    long next3 = sequencer.getNextSequence();

    assertEquals(next1 + 1, next2, "Next sequence should increment");
    assertEquals(next2 + 1, next3, "Next sequence should increment");
  }

  @Test
  public void testGetCursorReturnsCursor() {
    long cursor = sequencer.getCursor();
    assertTrue(cursor >= -1, "Cursor should be >= -1 initially");
  }

  @Test
  public void testGetJournalHandlerReturnsHandler() {
    assertNotNull(sequencer.getJournalHandler(), "Journal handler should not be null");
  }

  @Test
  public void testGetMatchingEngineHandlerReturnsHandler() {
    assertNotNull(
        sequencer.getMatchingEngineHandler(), "Matching engine handler should not be null");
  }

  @Test
  public void testGetJournalLatencyRecorderReturnsRecorder() {
    assertNotNull(
        sequencer.getJournalLatencyRecorder(), "Journal latency recorder should not be null");
  }

  @Test
  public void testGetMatchingLatencyRecorderReturnsRecorder() {
    assertNotNull(
        sequencer.getMatchingLatencyRecorder(), "Matching latency recorder should not be null");
  }

  @Test
  public void testGetEndToEndLatencyRecorderReturnsRecorder() {
    assertNotNull(
        sequencer.getEndToEndLatencyRecorder(), "End-to-end latency recorder should not be null");
  }

  @Test
  public void testGetNextSequenceAfterPublish() throws Exception {
    sequencer.publishOrder(
        new com.soby.sequencer.model.Order(
            1L,
            "AAPL",
            com.soby.sequencer.model.Side.BUY,
            com.soby.sequencer.model.OrderType.LIMIT,
            100L,
            10L));

    Thread.sleep(100);

    long next = sequencer.getNextSequence();
    assertTrue(next > 0, "Next sequence should be > 0 after publish");
  }

  @Test
  public void testGetEndToEndLatencyRecorderReturnsNonNull() {
    LatencyRecorder recorder = sequencer.getEndToEndLatencyRecorder();
    assertNotNull(recorder, "End-to-end latency recorder should be non-null");
  }
}
