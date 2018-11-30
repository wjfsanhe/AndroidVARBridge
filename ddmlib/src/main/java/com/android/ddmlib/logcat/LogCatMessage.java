 package com.android.ddmlib.logcat;
 
 import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
 
 public final class LogCatMessage
 {
   private final LogCatHeader mHeader;
   private final String mMessage;
 
   @Deprecated
   public LogCatMessage(Log.LogLevel logLevel, int pid, int tid, String appName, String tag, LogCatTimestamp timestamp, String msg)
   {
/*  45 */     this(new LogCatHeader(logLevel, pid, tid, appName, tag, timestamp), msg);
   }
 
   public LogCatMessage(LogCatHeader header, String msg)
   {
/*  52 */     this.mHeader = header;
/*  53 */     this.mMessage = msg;
   }
 
   public LogCatMessage(Log.LogLevel logLevel, String message)
   {
/*  61 */     this(logLevel, -1, -1, "", "", LogCatTimestamp.ZERO, message);
   }
 
   public LogCatHeader getHeader()
   {
/*  67 */     return this.mHeader;
   }
 
   public String getMessage()
   {
/*  72 */     return this.mMessage;
   }
 
   public Log.LogLevel getLogLevel()
   {
/*  77 */     return this.mHeader.getLogLevel();
   }
 
   public int getPid() {
/*  81 */     return this.mHeader.getPid();
   }
 
   public int getTid() {
/*  85 */     return this.mHeader.getTid();
   }
 
   public String getAppName()
   {
/*  90 */     return this.mHeader.getAppName();
   }
 
   public String getTag()
   {
/*  95 */     return this.mHeader.getTag();
   }
 
   public LogCatTimestamp getTimestamp()
   {
/* 100 */     return this.mHeader.getTimestamp();
   }
 
   public String toString()
   {
/* 105 */     return this.mHeader.toString() + ": " + this.mMessage;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatMessage
 * JD-Core Version:    0.6.2
 */