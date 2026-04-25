# SobySequencer

A production-quality Java low-latency event sequencer built on the LMAX Disruptor.

```
   ()__()
   (='.'=)    <--- Soby
   (")_(")
```

## Requirements

- Java 21+
- Gradle (wrapper included)
- [Just](https://github.com/casey/just) (optional, for simplified command recipes)

## Build

```bash
./gradlew build
```

Or using Just:

```bash
just build
```

## Run

```bash
./gradlew run
```

Or using Just:

```bash
just run-default
```

## Test

```bash
./gradlew test
```

Or using Just:

```bash
just test
```

## Code Formatting

This project uses Google Java Style with Spotless. To format your code:

```bash
./gradlew spotlessApply
```

Or using Just:

```bash
just format
```

To check if code is properly formatted (used in CI):

```bash
./gradlew spotlessCheck
```

Or using Just:

```bash
just format-check
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

## Recipes (Using Just)

Just provides a simplified, readable way to run common commands. The `justfile` includes:

### Build & Test

| Command | Description |
|---------|-------------|
| `just build` | Run full build with tests |
| `just test` | Run tests only |
| `just clean` | Clean build artifacts |
| `just clean-build` | Clean and rebuild |

### Formatting

| Command | Description |
|---------|-------------|
| `just format` | Apply Google Java formatting |
| `just format-check` | Check formatting (CI-friendly) |

### Running with Different Configurations

| Command | Description |
|---------|-------------|
| `just run-default` | Run with default config |
| `just run-small-ring-buffer` | Run with 1024 ring buffer |
| `just run-large-ring-buffer` | Run with 8192 ring buffer |
| `just run-huge-ring-buffer` | Run with 16384 ring buffer |
| `just run-busy-spin` | Use busy-spin wait strategy (lowest latency) |
| `just run-yielding` | Use yielding wait strategy |
| `just run-blocking` | Use blocking wait strategy (lowest CPU) |
| `just run-no-affinity` | Disable CPU affinity |
| `just run-cpu-1` | Pin to CPU core 1 |
| `just run-cpu-2` | Pin to CPU core 2 |

### Benchmark Recipes

| Command | Description |
|---------|-------------|
| `just run-warmup-10k-benchmark-100k` | 10k warmup, 100k benchmark |
| `just run-warmup-100k-benchmark-1m` | 100k warmup, 1M benchmark |

### Distribution

| Command | Description |
|---------|-------------|
| `just dist` | Create zip distribution |
| `just dist-tar` | Create tar distribution |
| `just quick-build` | Build without tests |
| `just quick-test` | Run tests without build |

## Architecture

See `ARCHITECTURE.md` for detailed technical documentation.

## License

See LICENSE file.
