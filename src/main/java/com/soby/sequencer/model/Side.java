package com.soby.sequencer.model;

/** Side of an order: BUY or SELL */
public enum Side {
  BUY((byte) 0),
  SELL((byte) 1);

  private final byte value;

  Side(byte value) {
    this.value = value;
  }

  public byte getValue() {
    return value;
  }
}
