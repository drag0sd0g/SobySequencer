package com.soby.sequencer.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class AffinitySupportTest {

  @Test
  public void testPinCurrentThreadToCoreDoesNotThrow() {
    AffinitySupport.pinCurrentThreadToCore(0);
  }

  @Test
  public void testPinToAnyCoreDoesNotThrow() {
    AffinitySupport.pinToAnyCore();
  }

  @Test
  public void testPinToNegativeCoreDoesNotThrow() {
    AffinitySupport.pinCurrentThreadToCore(-1);
  }

  @Test
  public void testPinToTooHighCoreDoesNotThrow() {
    int maxCores = Runtime.getRuntime().availableProcessors();
    AffinitySupport.pinCurrentThreadToCore(maxCores + 100);
  }

  @Test
  public void testClassExists() {
    assertNotNull(AffinitySupport.class);
  }
}
