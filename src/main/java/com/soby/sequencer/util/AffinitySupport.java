package com.soby.sequencer.util;

import java.util.ArrayList;
import java.util.List;
import net.openhft.affinity.AffinityLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps net.openhft.affinity.AffinityLock for CPU thread affinity. Holds acquired locks for the
 * lifetime of the process so affinity is not inadvertently released.
 */
public class AffinitySupport {
  private static final Logger LOG = LoggerFactory.getLogger(AffinitySupport.class);

  // Locks must be held (not closed) to keep the thread pinned; closed locks release affinity.
  private static final List<AffinityLock> heldLocks = new ArrayList<>();

  /**
   * Pin the current thread to a specific CPU core. The affinity is held until the JVM exits. Has no
   * effect if the core is unavailable or affinity is not supported on this platform.
   *
   * @param core the CPU core number to pin to
   */
  public static synchronized void pinCurrentThreadToCore(int core) {
    try {
      AffinityLock lock = AffinityLock.acquireLock(core);
      if (lock.cpuId() >= 0) {
        heldLocks.add(lock);
        renameThreadWithCore(lock.cpuId());
      } else {
        lock.close();
        LOG.warn("Failed to acquire affinity lock for core {}", core);
      }
    } catch (Throwable e) {
      LOG.warn("Failed to pin thread to core {}: {}", core, e.getMessage());
    }
  }

  /** Pin the current thread to any available CPU core. The affinity is held until the JVM exits. */
  public static synchronized void pinToAnyCore() {
    try {
      AffinityLock lock = AffinityLock.acquireLock();
      if (lock.cpuId() >= 0) {
        heldLocks.add(lock);
        renameThreadWithCore(lock.cpuId());
      } else {
        lock.close();
        LOG.warn("Failed to acquire affinity lock for any core");
      }
    } catch (Throwable e) {
      LOG.warn("Failed to pin thread to core: {}", e.getMessage());
    }
  }

  private static void renameThreadWithCore(int coreId) {
    var threadName = Thread.currentThread().getName();
    Thread.currentThread().setName(threadName + " [cpu " + coreId + "]");
  }
}
