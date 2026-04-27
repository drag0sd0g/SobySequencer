# SobySequencer

```
   ()__()
  (='.'=)
  (")_(")
```

A production-quality, low-latency event sequencer for Java, built on the [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/) pattern. It models the core of a trading infrastructure component: a high-throughput order ingestion pipeline with durable journaling, parallel stage processing, and price-time priority matching.

**Key characteristics:**
- p99.9 latencies in the low-microsecond range on modern hardware
- 1M–10M events/sec throughput (single producer)
- Zero GC pressure on the hot path — fully pre-allocated ring buffer
- Diamond dependency pipeline: journal ∥ replica → match → output

---

## Requirements

- Java 21+
- Gradle (wrapper included — no local Gradle installation needed)
- [Just](https://github.com/casey/just) *(optional, for simplified command recipes)*

---

## Quick Start

```bash
# Build and run tests
./gradlew build

# Run the benchmark with default configuration
./gradlew run
```

Expected output:

```
[SobySequencer] Warmup completed at 1,234,567.89 events/sec
[SobySequencer] Benchmark completed at 987,654.32 events/sec

Journal Handler Latency:
  count:  1,000,000
  mean:   1,234 ns
  p50:      980 ns
  p99:    2,304 ns
  p99.9:  4,096 ns
  max:  125,312 ns

Matching Engine Handler Latency:
  count:  1,000,000
  mean:     312 ns
  p50:      256 ns
  p99:      768 ns
  p99.9:  1,536 ns
  max:   45,056 ns
```

---

## Configuration

Pass configuration as system properties:

```bash
./gradlew run \
  -DringBufferSize=8192 \
  -DwaitStrategy=YIELDING \
  -DenableAffinity=false
```

| Property | Default | Description |
|----------|---------|-------------|
| `ringBufferSize` | `4096` | Ring buffer capacity; automatically rounded up to the next power of 2 |
| `waitStrategy` | `BUSY_SPIN` | `BUSY_SPIN` / `YIELDING` / `SLEEPING` / `BLOCKING` — see [Architecture](docs/ARCHITECTURE.md#6-wait-strategies) |
| `enableAffinity` | `true` | Pin the producer thread to a specific CPU core |
| `cpuCore` | `0` | CPU core index for thread affinity |
| `warmupPublishCount` | `100000` | Events published in the warm-up phase (discarded from latency stats) |
| `benchmarkPublishCount` | `1000000` | Events published in the measured benchmark phase |

**Choosing a wait strategy:**

| Environment | Recommended strategy | Reason |
|-------------|---------------------|--------|
| HFT production, dedicated cores | `BUSY_SPIN` | Lowest latency (~100–200 ns detection) |
| Shared cores, general production | `YIELDING` | Good latency/CPU balance |
| Batch / power-constrained | `SLEEPING` | Minimal CPU usage |
| Unit tests / CI | `BLOCKING` | Zero CPU spin, fast CI |

---

## Building and Testing

```bash
# Full build (compile + test + format check + coverage)
./gradlew build

# Run tests only
./gradlew test

# Apply Google Java Style formatting
./gradlew spotlessApply

# Verify formatting (used in CI)
./gradlew spotlessCheck
```

Code coverage is enforced at **70% line coverage** via JaCoCo. Format is enforced via [Spotless](https://github.com/diffplug/spotless) with `google-java-format 1.19.2`.

---

## Just Recipes

[Just](https://github.com/casey/just) provides convenient shorthand for common workflows. Run `just help` to list all recipes.

### Build & test

| Command | Description |
|---------|-------------|
| `just build` | Full build with tests |
| `just test` | Tests only |
| `just clean` | Delete build artifacts |
| `just clean-build` | Clean then full build |
| `just quick-build` | Compile only, skip tests |

### Formatting

| Command | Description |
|---------|-------------|
| `just format` | Apply Google Java formatting |
| `just format-check` | Verify formatting (CI-friendly) |

### Running benchmarks

| Command | Description |
|---------|-------------|
| `just run-default` | Default configuration |
| `just run-busy-spin` | Busy-spin wait strategy (lowest latency) |
| `just run-yielding` | Yielding wait strategy |
| `just run-blocking` | Blocking wait strategy (lowest CPU) |
| `just run-no-affinity` | Disable CPU affinity |
| `just run-cpu-1` | Pin to CPU core 1 |
| `just run-cpu-2` | Pin to CPU core 2 |
| `just run-small-ring-buffer` | 1 024-slot ring buffer |
| `just run-large-ring-buffer` | 8 192-slot ring buffer |
| `just run-huge-ring-buffer` | 16 384-slot ring buffer |
| `just run-warmup-10k-benchmark-100k` | 10k warm-up, 100k benchmark |
| `just run-warmup-100k-benchmark-1m` | 100k warm-up, 1M benchmark |

### Distribution

| Command | Description |
|---------|-------------|
| `just dist` | Create a `.zip` distribution |
| `just dist-tar` | Create a `.tar` distribution |

---

## Architecture

For a detailed technical description of the Disruptor pattern, ring buffer mechanics, sequence barriers, handler pipeline, journal design, matching engine, latency measurement methodology, and production hardening guidelines, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## License

See [LICENSE](LICENSE).
