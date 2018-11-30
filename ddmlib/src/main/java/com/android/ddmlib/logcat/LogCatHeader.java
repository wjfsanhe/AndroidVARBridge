 package com.android.ddmlib.logcat;
 
 import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
 
 public final class LogCatHeader
 {
   private final Log.LogLevel mLogLevel;
   private final int mPid;
   private final int mTid;
   private final String mAppName;
   private final String mTag;
   private final LogCatTimestamp mTimestamp;
 
   public LogCatHeader(Log.LogLevel logLevel, int pid, int tid, String appName, String tag, LogCatTimestamp timestamp)
   {
/* 48 */     this.mLogLevel = logLevel;
/* 49 */     this.mAppName = appName;
/* 50 */     this.mTag = tag;
/* 51 */     this.mTimestamp = timestamp;
/* 52 */     this.mPid = pid;
/* 53 */     this.mTid = tid;
   }
 
   public Log.LogLevel getLogLevel()
   {
/* 58 */     return this.mLogLevel;
   }
 
   public int getPid() {
/* 62 */     return this.mPid;
   }
 
   public int getTid() {
/* 66 */     return this.mTid;
   }
 
   public String getAppName()
   {
/* 71 */     return this.mAppName;
   }
 
   public String getTag()
   {
/* 76 */     return this.mTag;
   }
 
   public LogCatTimestamp getTimestamp()
   {
/* 81 */     return this.mTimestamp;
   }
 
   public String toString()
   {
/* 86 */     return String.format("%s: %s/%s(%s)", new Object[] { this.mTimestamp, Character.valueOf(this.mLogLevel.getPriorityLetter()), this.mTag, Integer.valueOf(this.mPid) });
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatHeader
 * JD-Core Version:    0.6.2
 */