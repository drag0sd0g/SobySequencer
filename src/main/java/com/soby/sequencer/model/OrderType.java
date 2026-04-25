package com.soby.sequencer.model;

/** Type of order: MARKET or LIMIT */
public enum OrderType {
  MARKET((byte) 0),
  LIMIT((byte) 1);

  private final byte value;

  OrderType(byte value) {
    this.value = value;
  }

  public byte getValue() {
    return value;
  }
}
