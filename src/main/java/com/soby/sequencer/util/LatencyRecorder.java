package com.soby.sequencer.util;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.SynchronizedHistogram;

/**
 * Wraps HdrHistogram for recording and reporting nanosecond-resolution latencies. Thread-safe via
 * SynchronizedHistogram. Used by each handler stage to measure its contribution to total pipeline
 * latency.
 */
public class LatencyRecorder {
  private final Histogram histogram;
  private final long highestTrackableValue;

  public LatencyRecorder(long highestTrackableValue) {
    this.highestTrackableValue = highestTrackableValue;
    this.histogram = new SynchronizedHistogram(highestTrackableValue, 5);
  }

  /**
   * Record a latency value in nanoseconds.
   *
   * @param latencyNanos latency in nanoseconds
   */
  public void record(long latencyNanos) {
    if (latencyNanos >= 0 && latencyNanos <= highestTrackableValue) {
      histogram.recordValue(latencyNanos);
    } else if (latencyNanos > highestTrackableValue) {
      histogram.recordValue(highestTrackableValue);
    }
  }

  /**
   * Print a latency report to stdout with percentile statistics.
   *
   * @param label label to prefix the report output
   */
  public void printReport(String label) {
    System.out.println(label + " Latency:");
    System.out.println("  p50:    " + format(histogram.getValueAtPercentile(50.0)) + " ns");
    System.out.println("  p95:    " + format(histogram.getValueAtPercentile(95.0)) + " ns");
    System.out.println("  p99:    " + format(histogram.getValueAtPercentile(99.0)) + " ns");
    System.out.println("  p99.9:  " + format(histogram.getValueAtPercentile(99.9)) + " ns");
    System.out.println("  p99.99: " + format(histogram.getValueAtPercentile(99.99)) + " ns");
    System.out.println("  max:    " + format(histogram.getMaxValue()) + " ns");
  }

  private String format(long value) {
    return String.format("%,d", value);
  }

  /** Reset the histogram, clearing all recorded values. */
  public void reset() {
    histogram.reset();
  }
}
