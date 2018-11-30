package com.android.ddmlib.testrunner;

import java.util.Map;

public abstract interface ITestRunListener
{
  public abstract void testRunStarted(String paramString, int paramInt);

  public abstract void testStarted(TestIdentifier paramTestIdentifier);

  public abstract void testFailed(TestIdentifier paramTestIdentifier, String paramString);

  public abstract void testAssumptionFailure(TestIdentifier paramTestIdentifier, String paramString);

  public abstract void testIgnored(TestIdentifier paramTestIdentifier);

  public abstract void testEnded(TestIdentifier paramTestIdentifier, Map<String, String> paramMap);

  public abstract void testRunFailed(String paramString);

  public abstract void testRunStopped(long paramLong);

  public abstract void testRunEnded(long paramLong, Map<String, String> paramMap);
}

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.ITestRunListener
 * JD-Core Version:    0.6.2
 */