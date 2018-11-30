package com.android.ddmlib;

public abstract interface IShellOutputReceiver
{
  public abstract void addOutput(byte[] paramArrayOfByte, int paramInt1, int paramInt2);

  public abstract void flush();

  public abstract boolean isCancelled();
}

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.IShellOutputReceiver
 * JD-Core Version:    0.6.2
 */