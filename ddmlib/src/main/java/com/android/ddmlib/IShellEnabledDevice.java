package com.android.ddmlib;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract interface IShellEnabledDevice
{
  public abstract String getName();

  public abstract void executeShellCommand(String paramString, IShellOutputReceiver paramIShellOutputReceiver, long paramLong, TimeUnit paramTimeUnit)
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;

  public abstract Future<String> getSystemProperty(String paramString);
}

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.IShellEnabledDevice
 * JD-Core Version:    0.6.2
 */