# SobySequencer - Development Recipes
# https://github.com/dragos/SobySequencer

# Build and test recipes
build:
    ./gradlew build

test:
    ./gradlew test

# Code formatting
format:
    ./gradlew spotlessApply

format-check:
    ./gradlew spotlessCheck

# Run with default configuration
run-default:
    ./gradlew run

# Run with different ring buffer sizes
run-small-ring-buffer:
    ./gradlew run -DringBufferSize=1024

run-large-ring-buffer:
    ./gradlew run -DringBufferSize=8192

run-huge-ring-buffer:
    ./gradlew run -DringBufferSize=16384

# Run with different wait strategies
run-busy-spin:
    ./gradlew run -DwaitStrategy=BUSY_SPIN

run-yielding:
    ./gradlew run -DwaitStrategy=YIELDING

run-blocking:
    ./gradlew run -DwaitStrategy=BLOCKING

# Run with different CPU configurations
run-no-affinity:
    ./gradlew run -DenableAffinity=false

run-cpu-1:
    ./gradlew run -DcpuCore=1

run-cpu-2:
    ./gradlew run -DcpuCore=2

# Run benchmark with custom counts
run-warmup-10k-benchmark-100k:
    ./gradlew run -DwarmupPublishCount=10000 -DbenchmarkPublishCount=100000

run-warmup-100k-benchmark-1m:
    ./gradlew run -DwarmupPublishCount=100000 -DbenchmarkPublishCount=1000000

# Clean and rebuild
clean:
    ./gradlew clean

clean-build:
    ./gradlew clean build

# Distribution
dist:
    ./gradlew distZip

dist-tar:
    ./gradlew distTar

# Quick build (no tests)
quick-build:
    ./gradlew assemble

# Quick test (no build)
quick-test:
    ./gradlew test

# Show help
help:
    @echo "SobySequencer Recipes"
    @echo ""
    @echo "Build & Test:"
    @echo "  just build          - Run full build with tests"
    @echo "  just test           - Run tests only"
    @echo "  just clean          - Clean build artifacts"
    @echo ""
    @echo "Formatting:"
    @echo "  just format         - Apply Google Java formatting"
    @echo "  just format-check   - Check formatting (CI-friendly)"
    @echo ""
    @echo "Running:"
    @echo "  just run-default    - Run with default config"
    @echo "  just run-small-ring-buffer  - Run with 1024 ring buffer"
    @echo "  just run-large-ring-buffer  - Run with 8192 ring buffer"
    @echo "  just run-huge-ring-buffer   - Run with 16384 ring buffer"
    @echo "  just run-busy-spin  - Use busy-spin wait strategy (lowest latency)"
    @echo "  just run-yielding   - Use yielding wait strategy"
    @echo "  just run-blocking   - Use blocking wait strategy (lowest CPU)"
    @echo "  just run-no-affinity - Disable CPU affinity"
    @echo "  just run-cpu-1      - Pin to CPU core 1"
    @echo "  just run-cpu-2      - Pin to CPU core 2"
    @echo ""
    @echo "Benchmarks:"
    @echo "  just run-warmup-10k-benchmark-100k - 10k warmup, 100k benchmark"
    @echo "  just run-warmup-100k-benchmark-1m  - 100k warmup, 1M benchmark"
    @echo ""
    @echo "Distribution:"
    @echo "  just dist           - Create zip distribution"
    @echo "  just dist-tar       - Create tar distribution"
    @echo ""
