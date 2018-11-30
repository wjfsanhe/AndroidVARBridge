package com.android.ddmlib.logcat;

import java.util.List;

public abstract interface LogCatListener
{
  public abstract void log(List<LogCatMessage> paramList);
}

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatListener
 * JD-Core Version:    0.6.2
 */