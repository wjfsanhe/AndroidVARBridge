 package com.android.ddmlib.testrunner;
 
 import com.android.ddmlib.AdbCommandRejectedException;
 import com.android.ddmlib.ShellCommandUnresponsiveException;
 import com.android.ddmlib.TimeoutException;
 import java.io.IOException;
 import java.util.Collection;
 import java.util.concurrent.TimeUnit;
 
 public abstract interface IRemoteAndroidTestRunner
 {
   public abstract String getPackageName();
 
   public abstract String getRunnerName();
 
   public abstract void setClassName(String paramString);
 
   public abstract void setClassNames(String[] paramArrayOfString);
 
   public abstract void setMethodName(String paramString1, String paramString2);
 
   public abstract void setTestPackageName(String paramString);
 
   public abstract void setTestSize(TestSize paramTestSize);
 
   public abstract void addInstrumentationArg(String paramString1, String paramString2);
 
   public abstract void removeInstrumentationArg(String paramString);
 
   public abstract void addBooleanArg(String paramString, boolean paramBoolean);
 
   public abstract void setLogOnly(boolean paramBoolean);
 
   public abstract void setDebug(boolean paramBoolean);
 
   public abstract void setCoverage(boolean paramBoolean);
 
   public abstract void setTestCollection(boolean paramBoolean);
 
   @Deprecated
   public abstract void setMaxtimeToOutputResponse(int paramInt);
 
   public abstract void setMaxTimeToOutputResponse(long paramLong, TimeUnit paramTimeUnit);
 
   public abstract void setRunName(String paramString);
 
   public abstract void run(ITestRunListener[] paramArrayOfITestRunListener)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
 
   public abstract void run(Collection<ITestRunListener> paramCollection)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
 
   public abstract void cancel();
 
   public static enum TestSize
   {
/* 35 */     SMALL("small"), 
 
/* 37 */     MEDIUM("medium"), 
 
/* 39 */     LARGE("large");
 
     private String mRunnerValue;
 
     private TestSize(String runnerValue)
     {
/* 50 */       this.mRunnerValue = runnerValue;
     }
 
     String getRunnerValue() {
/* 54 */       return this.mRunnerValue;
     }
 
     public static TestSize getTestSize(String value)
     {
/* 64 */       StringBuilder msgBuilder = new StringBuilder("Unknown TestSize ");
/* 65 */       msgBuilder.append(value);
/* 66 */       msgBuilder.append(", Must be one of ");
/* 67 */       for (TestSize size : values()) {
/* 68 */         if (size.getRunnerValue().equals(value)) {
/* 69 */           return size;
         }
/* 71 */         msgBuilder.append(size.getRunnerValue());
/* 72 */         msgBuilder.append(", ");
       }
/* 74 */       throw new IllegalArgumentException(msgBuilder.toString());
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.IRemoteAndroidTestRunner
 * JD-Core Version:    0.6.2
 */