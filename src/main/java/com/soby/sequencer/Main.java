package com.soby.sequencer;

import com.soby.sequencer.producer.OrderProducer;
import com.soby.sequencer.util.AffinitySupport;

/**
 * Main entry point for the SobySequencer application. Runs warmup and benchmark phases, printing
 * latency reports.
 */
public class Main {
  public static void main(String[] args) {
    System.out.println("[SobySequencer] Starting up...");
    System.out.println("[SobySequencer] Java " + System.getProperty("java.version"));
    System.out.println("[SobySequencer] LMAX Disruptor sequencer");

    // Build configuration from system properties or defaults
    SequencerConfig config = buildConfig(args);

    System.out.println("[SobySequencer] Configuration: " + config);

    try {
      Sequencer sequencer = new Sequencer(config);

      // Optionally pin to CPU core for lowest latency
      if (config.isEnableAffinity()) {
        System.out.println("[SobySequencer] Pinning to CPU core " + config.getSequencerCpuCore());
        AffinitySupport.pinCurrentThreadToCore(config.getSequencerCpuCore());
      } else {
        System.out.println("[SobySequencer] CPU affinity disabled");
      }

      // Start the sequencer
      sequencer.start();
      System.out.println("[SobySequencer] Sequencer started");

      // Create producer for benchmarking
      OrderProducer producer = new OrderProducer(sequencer, config.getBenchmarkPublishCount());

      // Warmup phase - don't record latencies
      System.out.println(
          "[SobySequencer] Warmup: " + config.getWarmupPublishCount() + " events published");
      long warmupStart = System.nanoTime();
      for (long i = 0; i < config.getWarmupPublishCount(); i++) {
        producer.publishOrder(i);
      }
      long warmupEnd = System.nanoTime();
      double warmupRate =
          config.getWarmupPublishCount() / ((warmupEnd - warmupStart) / 1_000_000_000.0);
      System.out.printf("[SobySequencer] Warmup completed at %.2f events/sec%n", warmupRate);

      // Benchmark phase - record latencies
      System.out.println(
          "[SobySequencer] Benchmark: " + config.getBenchmarkPublishCount() + " events published");
      long benchmarkStart = System.nanoTime();
      for (long i = 0; i < config.getBenchmarkPublishCount(); i++) {
        producer.publishOrder(i + config.getWarmupPublishCount());
      }
      long benchmarkEnd = System.nanoTime();
      double benchmarkRate =
          config.getBenchmarkPublishCount() / ((benchmarkEnd - benchmarkStart) / 1_000_000_000.0);
      System.out.printf("[SobySequencer] Benchmark completed at %.2f events/sec%n", benchmarkRate);

      // Print latency reports
      System.out.println();
      sequencer.getJournalLatencyRecorder().printReport("Journal Handler");
      System.out.println();
      sequencer.getMatchingLatencyRecorder().printReport("Matching Engine Handler");
      System.out.println();

      // Shutdown
      sequencer.shutdown();
      System.out.println("[SobySequencer] Shutdown complete");
      System.out.println(
          "[SobySequencer] Total throughput: "
              + String.format("%.2f", benchmarkRate)
              + " events/sec");

    } catch (Exception e) {
      System.err.println("[SobySequencer] Fatal error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Build configuration from system properties.
   *
   * @param args command line arguments (unused)
   * @return configured SequencerConfig
   */
  private static SequencerConfig buildConfig(String[] args) {
    SequencerConfig.Builder builder = new SequencerConfig.Builder();

    String ringBufferSize = System.getProperty("ringBufferSize");
    if (ringBufferSize != null) {
      builder.ringBufferSize(Integer.parseInt(ringBufferSize));
    }

    String waitStrategy = System.getProperty("waitStrategy");
    if (waitStrategy != null) {
      builder.waitStrategy(SequencerConfig.WaitStrategyType.valueOf(waitStrategy));
    }

    String enableAffinity = System.getProperty("enableAffinity");
    if (enableAffinity != null) {
      builder.enableAffinity(Boolean.parseBoolean(enableAffinity));
    }

    String cpuCore = System.getProperty("cpuCore");
    if (cpuCore != null) {
      builder.sequencerCpuCore(Integer.parseInt(cpuCore));
    }

    String warmupCount = System.getProperty("warmupPublishCount");
    if (warmupCount != null) {
      builder.warmupPublishCount(Integer.parseInt(warmupCount));
    }

    String benchmarkCount = System.getProperty("benchmarkPublishCount");
    if (benchmarkCount != null) {
      builder.benchmarkPublishCount(Integer.parseInt(benchmarkCount));
    }

    return builder.build();
  }
}
