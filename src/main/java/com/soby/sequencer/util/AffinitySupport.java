package com.soby.sequencer.util;

import net.openhft.affinity.AffinityLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps net.openhft.affinity.AffinityLock for CPU thread affinity. Provides methods to pin the
 * current thread to specific CPU cores.
 */
public class AffinitySupport {
  private static final Logger LOG = LoggerFactory.getLogger(AffinitySupport.class);

  /**
   * Pin the current thread to a specific CPU core.
   *
   * @param core the CPU core number to pin to
   */
  public static void pinCurrentThreadToCore(int core) {
    try (var affinityLock = AffinityLock.acquireLock(core)) {
      if (affinityLock.cpuId() >= 0) {
        pinThreadToCore(affinityLock.cpuId());
      } else {
        LOG.warn("Failed to acquire affinity lock for core {}", core);
      }
    } catch (Throwable e) {
      LOG.warn("Failed to pin thread to core {}: {}", core, e.getMessage());
    }
  }

  /**
   * Pin the current thread to any available CPU core. Falls back gracefully if affinity cannot be
   * set.
   */
  public static void pinToAnyCore() {
    try (var affinityLock = AffinityLock.acquireLock()) {
      if (affinityLock.cpuId() >= 0) {
        pinThreadToCore(affinityLock.cpuId());
      } else {
        LOG.warn("Failed to acquire affinity lock for any core");
      }
    } catch (Throwable e) {
      LOG.warn("Failed to pin thread to core: {}", e.getMessage());
    }
  }

  private static void pinThreadToCore(int coreId) {
    var threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(threadName + " [cpu " + coreId + "]");
  }
}
