package com.soby.sequencer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SequencerConfigTest {

  @Test
  public void testBuilderCreatesValidConfig() {
    SequencerConfig config =
        new SequencerConfig.Builder()
            .ringBufferSize(1024)
            .waitStrategy(SequencerConfig.WaitStrategyType.BLOCKING)
            .enableAffinity(false)
            .sequencerCpuCore(2)
            .journalFilePath("/tmp/test.dat")
            .warmupPublishCount(1000)
            .benchmarkPublishCount(10000)
            .build();

    assertNotNull(config, "Config should not be null");
    assertEquals(1024, config.getRingBufferSize(), "Ring buffer size should match");
    assertFalse(config.isEnableAffinity(), "Enable affinity should match");
    assertEquals(2, config.getSequencerCpuCore(), "CPU core should match");
    assertEquals("/tmp/test.dat", config.getJournalFilePath(), "Journal path should match");
    assertEquals(1000, config.getWarmupPublishCount(), "Warmup count should match");
    assertEquals(10000, config.getBenchmarkPublishCount(), "Benchmark count should match");
  }

  @Test
  public void testBuilderRoundsToPowerOfTwo() {
    SequencerConfig config = new SequencerConfig.Builder().ringBufferSize(1000).build();

    assertEquals(1024, config.getRingBufferSize(), "Should round up to 1024");
  }

  @Test
  public void testBuilderAlreadyPowerOfTwo() {
    SequencerConfig config = new SequencerConfig.Builder().ringBufferSize(2048).build();

    assertEquals(2048, config.getRingBufferSize(), "Should keep 2048");
  }

  @Test
  public void testBuilderMinSize() {
    SequencerConfig config = new SequencerConfig.Builder().ringBufferSize(1).build();

    assertEquals(1, config.getRingBufferSize(), "Should keep 1");
  }

  @Test
  public void testBuilderDefaultValues() {
    SequencerConfig config = new SequencerConfig.Builder().build();

    assertEquals(
        SequencerConfig.DEFAULT_RING_BUFFER_SIZE, config.getRingBufferSize(), "Default ring size");
    assertEquals(
        SequencerConfig.DEFAULT_ENABLE_AFFINITY,
        config.isEnableAffinity(),
        "Default enable affinity");
    assertEquals(
        SequencerConfig.DEFAULT_CPU_CORE, config.getSequencerCpuCore(), "Default CPU core");
    assertEquals(
        SequencerConfig.DEFAULT_JOURNAL_FILE_PATH,
        config.getJournalFilePath(),
        "Default journal path");
    assertEquals(
        SequencerConfig.DEFAULT_WARMUP_COUNT,
        config.getWarmupPublishCount(),
        "Default warmup count");
    assertEquals(
        SequencerConfig.DEFAULT_BENCHMARK_COUNT,
        config.getBenchmarkPublishCount(),
        "Default benchmark count");
  }
}
