 package com.android.ddmlib.testrunner;
 
 import com.android.ddmlib.AdbCommandRejectedException;
 import com.android.ddmlib.IShellEnabledDevice;
 import com.android.ddmlib.Log;
 import com.android.ddmlib.ShellCommandUnresponsiveException;
 import com.android.ddmlib.TimeoutException;
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Hashtable;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 
 public class RemoteAndroidTestRunner
   implements IRemoteAndroidTestRunner
 {
   private final String mPackageName;
   private final String mRunnerName;
   private IShellEnabledDevice mRemoteDevice;
/*  44 */   private long mMaxTimeToOutputResponse = 0L;
/*  45 */   private TimeUnit mMaxTimeUnits = TimeUnit.MILLISECONDS;
/*  46 */   private String mRunName = null;
   private Map<String, String> mArgMap;
   private InstrumentationResultParser mParser;
   private static final String LOG_TAG = "RemoteAndroidTest";
   private static final String DEFAULT_RUNNER_NAME = "android.test.InstrumentationTestRunner";
   private static final char CLASS_SEPARATOR = ',';
   private static final char METHOD_SEPARATOR = '#';
   private static final char RUNNER_SEPARATOR = '/';
   private static final String CLASS_ARG_NAME = "class";
   private static final String LOG_ARG_NAME = "log";
   private static final String DEBUG_ARG_NAME = "debug";
   private static final String COVERAGE_ARG_NAME = "coverage";
   private static final String PACKAGE_ARG_NAME = "package";
   private static final String SIZE_ARG_NAME = "size";
   private static final String DELAY_MSEC_ARG_NAME = "delay_msec";
/*  67 */   private String mRunOptions = "";
   private static final int TEST_COLLECTION_TIMEOUT = 120000;
 
   public RemoteAndroidTestRunner(String packageName, String runnerName, IShellEnabledDevice remoteDevice)
   {
/*  83 */     this.mPackageName = packageName;
/*  84 */     this.mRunnerName = runnerName;
/*  85 */     this.mRemoteDevice = remoteDevice;
/*  86 */     this.mArgMap = new Hashtable();
   }
 
   public RemoteAndroidTestRunner(String packageName, IShellEnabledDevice remoteDevice)
   {
/*  97 */     this(packageName, null, remoteDevice);
   }
 
   public String getPackageName()
   {
/* 102 */     return this.mPackageName;
   }
 
   public String getRunnerName()
   {
/* 107 */     if (this.mRunnerName == null) {
/* 108 */       return "android.test.InstrumentationTestRunner";
     }
/* 110 */     return this.mRunnerName;
   }
 
   private String getRunnerPath()
   {
/* 117 */     return new StringBuilder().append(getPackageName()).append('/').append(getRunnerName()).toString();
   }
 
   public void setClassName(String className)
   {
/* 122 */     addInstrumentationArg("class", className);
   }
 
   public void setClassNames(String[] classNames)
   {
/* 127 */     StringBuilder classArgBuilder = new StringBuilder();
 
/* 129 */     for (int i = 0; i < classNames.length; i++) {
/* 130 */       if (i != 0) {
/* 131 */         classArgBuilder.append(',');
       }
/* 133 */       classArgBuilder.append(classNames[i]);
     }
/* 135 */     setClassName(classArgBuilder.toString());
   }
 
   public void setMethodName(String className, String testName)
   {
/* 140 */     setClassName(new StringBuilder().append(className).append('#').append(testName).toString());
   }
 
   public void setTestPackageName(String packageName)
   {
/* 145 */     addInstrumentationArg("package", packageName);
   }
 
   public void addInstrumentationArg(String name, String value)
   {
/* 150 */     if ((name == null) || (value == null)) {
/* 151 */       throw new IllegalArgumentException("name or value arguments cannot be null");
     }
/* 153 */     this.mArgMap.put(name, value);
   }
 
   public void removeInstrumentationArg(String name)
   {
/* 158 */     if (name == null) {
/* 159 */       throw new IllegalArgumentException("name argument cannot be null");
     }
/* 161 */     this.mArgMap.remove(name);
   }
 
   public void addBooleanArg(String name, boolean value)
   {
/* 166 */     addInstrumentationArg(name, Boolean.toString(value));
   }
 
   public void setLogOnly(boolean logOnly)
   {
/* 171 */     addBooleanArg("log", logOnly);
   }
 
   public void setDebug(boolean debug)
   {
/* 176 */     addBooleanArg("debug", debug);
   }
 
   public void setCoverage(boolean coverage)
   {
/* 181 */     addBooleanArg("coverage", coverage);
   }
 
   public void setTestSize(IRemoteAndroidTestRunner.TestSize size)
   {
/* 186 */     addInstrumentationArg("size", size.getRunnerValue());
   }
 
   public void setTestCollection(boolean collect)
   {
/* 191 */     if (collect)
     {
/* 193 */       setLogOnly(true);
 
/* 195 */       setMaxTimeToOutputResponse(120000L, TimeUnit.MILLISECONDS);
/* 196 */       if (getApiLevel() < 16)
       {
/* 199 */         addInstrumentationArg("delay_msec", "15");
       }
     } else {
/* 202 */       setLogOnly(false);
 
/* 204 */       setMaxTimeToOutputResponse(this.mMaxTimeToOutputResponse, this.mMaxTimeUnits);
/* 205 */       if (getApiLevel() < 16)
       {
/* 207 */         removeInstrumentationArg("delay_msec");
       }
     }
   }
 
   private int getApiLevel()
   {
     try
     {
/* 218 */       return Integer.parseInt((String)this.mRemoteDevice.getSystemProperty("ro.build.version.sdk").get());
     } catch (Exception e) {
     }
/* 221 */     return -1;
   }
 
   public void setMaxtimeToOutputResponse(int maxTimeToOutputResponse)
   {
/* 227 */     setMaxTimeToOutputResponse(maxTimeToOutputResponse, TimeUnit.MILLISECONDS);
   }
 
   public void setMaxTimeToOutputResponse(long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
   {
/* 232 */     this.mMaxTimeToOutputResponse = maxTimeToOutputResponse;
/* 233 */     this.mMaxTimeUnits = maxTimeUnits;
   }
 
   public void setRunName(String runName)
   {
/* 238 */     this.mRunName = runName;
   }
 
   public void run(ITestRunListener[] listeners)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 245 */     run(Arrays.asList(listeners));
   }
 
   public void run(Collection<ITestRunListener> listeners)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 252 */     String runCaseCommandStr = String.format("am instrument -w -r %1$s %2$s %3$s", new Object[] { getRunOptions(), getArgsCommand(), getRunnerPath() });
 
/* 254 */     Log.i("RemoteAndroidTest", String.format("Running %1$s on %2$s", new Object[] { runCaseCommandStr, this.mRemoteDevice.getName() }));
 
/* 256 */     String runName = this.mRunName == null ? this.mPackageName : this.mRunName;
/* 257 */     this.mParser = new InstrumentationResultParser(runName, listeners);
     try
     {
/* 260 */       this.mRemoteDevice.executeShellCommand(runCaseCommandStr, this.mParser, this.mMaxTimeToOutputResponse, this.mMaxTimeUnits);
     }
     catch (IOException e) {
/* 263 */       Log.w("RemoteAndroidTest", String.format("IOException %1$s when running tests %2$s on %3$s", new Object[] { e.toString(), getPackageName(), this.mRemoteDevice.getName() }));
 
/* 266 */       this.mParser.handleTestRunFailed(e.toString());
/* 267 */       throw e;
     } catch (ShellCommandUnresponsiveException e) {
/* 269 */       Log.w("RemoteAndroidTest", String.format("ShellCommandUnresponsiveException %1$s when running tests %2$s on %3$s", new Object[] { e.toString(), getPackageName(), this.mRemoteDevice.getName() }));
 
/* 272 */       this.mParser.handleTestRunFailed(String.format("Failed to receive adb shell test output within %1$d ms. Test may have timed out, or adb connection to device became unresponsive", new Object[] { Long.valueOf(this.mMaxTimeToOutputResponse) }));
 
/* 276 */       throw e;
     } catch (TimeoutException e) {
/* 278 */       Log.w("RemoteAndroidTest", String.format("TimeoutException when running tests %1$s on %2$s", new Object[] { getPackageName(), this.mRemoteDevice.getName() }));
 
/* 281 */       this.mParser.handleTestRunFailed(e.toString());
/* 282 */       throw e;
     } catch (AdbCommandRejectedException e) {
/* 284 */       Log.w("RemoteAndroidTest", String.format("AdbCommandRejectedException %1$s when running tests %2$s on %3$s", new Object[] { e.toString(), getPackageName(), this.mRemoteDevice.getName() }));
 
/* 287 */       this.mParser.handleTestRunFailed(e.toString());
/* 288 */       throw e;
     }
   }
 
   public String getRunOptions()
   {
/* 296 */     return this.mRunOptions;
   }
 
   public void setRunOptions(String options)
   {
/* 304 */     this.mRunOptions = options;
   }
 
   public void cancel()
   {
/* 309 */     if (this.mParser != null)
/* 310 */       this.mParser.cancel();
   }
 
   private String getArgsCommand()
   {
/* 320 */     StringBuilder commandBuilder = new StringBuilder();
/* 321 */     for (Map.Entry argPair : this.mArgMap.entrySet()) {
/* 322 */       String argCmd = String.format(" -e %1$s %2$s", new Object[] { argPair.getKey(), argPair.getValue() });
 
/* 324 */       commandBuilder.append(argCmd);
     }
/* 326 */     return commandBuilder.toString();
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.RemoteAndroidTestRunner
 * JD-Core Version:    0.6.2
 */