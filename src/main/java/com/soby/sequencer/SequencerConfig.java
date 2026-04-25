package com.soby.sequencer;

import java.util.Objects;

/**
 * Configuration for the Sequencer.
 * Immutable value object created via builder pattern.
 */
public class SequencerConfig {
    public static final int DEFAULT_RING_BUFFER_SIZE = 4096;
    public static final int DEFAULT_CPU_CORE = 0;
    public static final boolean DEFAULT_ENABLE_AFFINITY = true;
    public static final String DEFAULT_JOURNAL_FILE_PATH = "journal.dat";
    public static final int DEFAULT_WARMUP_COUNT = 100000;
    public static final int DEFAULT_BENCHMARK_COUNT = 1000000;

    private final int ringBufferSize;
    private final WaitStrategyType waitStrategy;
    private final boolean enableAffinity;
    private final int sequencerCpuCore;
    private final String journalFilePath;
    private final int warmupPublishCount;
    private final int benchmarkPublishCount;

    private SequencerConfig(Builder builder) {
        this.ringBufferSize = builder.ringBufferSize;
        this.waitStrategy = builder.waitStrategy;
        this.enableAffinity = builder.enableAffinity;
        this.sequencerCpuCore = builder.sequencerCpuCore;
        this.journalFilePath = builder.journalFilePath;
        this.warmupPublishCount = builder.warmupPublishCount;
        this.benchmarkPublishCount = builder.benchmarkPublishCount;
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }

    public WaitStrategyType getWaitStrategy() {
        return waitStrategy;
    }

    public boolean isEnableAffinity() {
        return enableAffinity;
    }

    public int getSequencerCpuCore() {
        return sequencerCpuCore;
    }

    public String getJournalFilePath() {
        return journalFilePath;
    }

    public int getWarmupPublishCount() {
        return warmupPublishCount;
    }

    public int getBenchmarkPublishCount() {
        return benchmarkPublishCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequencerConfig that = (SequencerConfig) o;
        return ringBufferSize == that.ringBufferSize &&
                enableAffinity == that.enableAffinity &&
                sequencerCpuCore == that.sequencerCpuCore &&
                warmupPublishCount == that.warmupPublishCount &&
                benchmarkPublishCount == that.benchmarkPublishCount &&
                waitStrategy == that.waitStrategy &&
                Objects.equals(journalFilePath, that.journalFilePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ringBufferSize, waitStrategy, enableAffinity, sequencerCpuCore, journalFilePath, warmupPublishCount, benchmarkPublishCount);
    }

    @Override
    public String toString() {
        return "SequencerConfig{" +
                "ringBufferSize=" + ringBufferSize +
                ", waitStrategy=" + waitStrategy +
                ", enableAffinity=" + enableAffinity +
                ", sequencerCpuCore=" + sequencerCpuCore +
                ", journalFilePath='" + journalFilePath + '\'' +
                ", warmupPublishCount=" + warmupPublishCount +
                ", benchmarkPublishCount=" + benchmarkPublishCount +
                '}';
    }

    public static class Builder {
        private int ringBufferSize = DEFAULT_RING_BUFFER_SIZE;
        private WaitStrategyType waitStrategy = WaitStrategyType.BUSY_SPIN;
        private boolean enableAffinity = DEFAULT_ENABLE_AFFINITY;
        private int sequencerCpuCore = DEFAULT_CPU_CORE;
        private String journalFilePath = DEFAULT_JOURNAL_FILE_PATH;
        private int warmupPublishCount = DEFAULT_WARMUP_COUNT;
        private int benchmarkPublishCount = DEFAULT_BENCHMARK_COUNT;

        public Builder ringBufferSize(int ringBufferSize) {
            this.ringBufferSize = ringBufferSize;
            return this;
        }

        public Builder waitStrategy(WaitStrategyType waitStrategy) {
            this.waitStrategy = waitStrategy;
            return this;
        }

        public Builder enableAffinity(boolean enableAffinity) {
            this.enableAffinity = enableAffinity;
            return this;
        }

        public Builder sequencerCpuCore(int sequencerCpuCore) {
            this.sequencerCpuCore = sequencerCpuCore;
            return this;
        }

        public Builder journalFilePath(String journalFilePath) {
            this.journalFilePath = journalFilePath;
            return this;
        }

        public Builder warmupPublishCount(int warmupPublishCount) {
            this.warmupPublishCount = warmupPublishCount;
            return this;
        }

        public Builder benchmarkPublishCount(int benchmarkPublishCount) {
            this.benchmarkPublishCount = benchmarkPublishCount;
            return this;
        }

        public SequencerConfig build() {
            // Ensure ring buffer size is a power of 2
            int size = ringBufferSize;
            if ((size & (size - 1)) != 0) {
                // Round up to next power of 2
                size = 1;
                while (size < ringBufferSize) {
                    size <<= 1;
                }
            }
            SequencerConfig config = new SequencerConfig(this);
            // Replace ring buffer size with correct power of 2
            return new SequencerConfig(new Builder()
                    .ringBufferSize(size)
                    .waitStrategy(config.waitStrategy)
                    .enableAffinity(config.enableAffinity)
                    .sequencerCpuCore(config.sequencerCpuCore)
                    .journalFilePath(config.journalFilePath)
                    .warmupPublishCount(config.warmupPublishCount)
                    .benchmarkPublishCount(config.benchmarkPublishCount));
        }

        public Builder() {}

        private Builder(Builder other) {
            this.ringBufferSize = other.ringBufferSize;
            this.waitStrategy = other.waitStrategy;
            this.enableAffinity = other.enableAffinity;
            this.sequencerCpuCore = other.sequencerCpuCore;
            this.journalFilePath = other.journalFilePath;
            this.warmupPublishCount = other.warmupPublishCount;
            this.benchmarkPublishCount = other.benchmarkPublishCount;
        }
    }

    /**
     * Wait strategy types for the disruptor.
     */
    public static enum WaitStrategyType {
        BUSY_SPIN,
        YIELDING,
        SLEEPING,
        BLOCKING
    }
}
