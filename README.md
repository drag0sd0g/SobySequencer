# SobySequencer

A production-quality Java low-latency event sequencer built on the LMAX Disruptor.

## Requirements

- Java 21+
- Gradle (wrapper included)

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew run
```

## Test

```bash
./gradlew test
```

## Code Formatting

This project uses Google Java Style with Spotless. To format your code:

```bash
./gradlew spotlessApply
```

To check if code is properly formatted (used in CI):

```bash
./gradlew spotlessCheck
```

## Configuration

Configuration is done via `SequencerConfig` in `Main.java` or via system properties:

```bash
./gradlew run -DringBufferSize=8192 -DwaitStrategy=YIELDING -DenableAffinity=false
```

### Configuration Options

- `ringBufferSize` - Ring buffer size (must be power of 2, default: 4096)
- `waitStrategy` - Wait strategy: BUSY_SPIN, YIELDING, SLEEPING, BLOCKING
- `enableAffinity` - Enable CPU thread affinity (default: true)
- `cpuCore` - CPU core to pin the sequencer thread
- `warmupPublishCount` - Number of orders for warmup phase (default: 100000)
- `benchmarkPublishCount` - Number of orders for benchmark phase (default: 1000000)

## Architecture

See `ARCHITECTURE.md` for detailed technical documentation.

## Project Structure

```
SobySequencer/
├── src/main/java/com/soby/sequencer/
│   ├── Main.java                     # Entry point
│   ├── Sequencer.java                # Core disruptor wrapper
│   ├── SequencerConfig.java          # Configuration
│   ├── event/                        # Ring buffer events
│   │   ├── OrderEvent.java
│   │   └── OrderEventFactory.java
│   ├── handler/                      # Event handlers
│   │   ├── JournalHandler.java
│   │   ├── MatchingEngineHandler.java
│   │   └── OutputHandler.java
│   ├── producer/                     # Order producers
│   │   └── OrderProducer.java
│   ├── model/                        # Domain models
│   │   ├── Order.java
│   │   ├── OrderType.java
│   │   └── Side.java
│   └── util/                         # Utilities
│       ├── AffinitySupport.java
│       └── LatencyRecorder.java
└── src/test/java/com/soby/sequencer/
    ├── SequencerIntegrationTest.java
    ├── MatchingEngineHandlerTest.java
    └── LatencyRecorderTest.java
```

## License

See LICENSE file.
