 package com.android.ddmlib.logcat;
 
 import com.android.ddmlib.IDevice;
 import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public final class LogCatMessageParser
 {
/*  43 */   private static final Pattern sLogHeaderPattern = Pattern.compile("^\\[\\s(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)\\s+(\\d*):\\s*(\\S+)\\s([VDIWEAF])/(.*[^\\s])\\s+\\]$");
   LogCatHeader mPrevHeader;
 
   public LogCatHeader processLogHeader(String line, IDevice device)
   {
/*  60 */     Matcher matcher = sLogHeaderPattern.matcher(line);
/*  61 */     if (!matcher.matches()) {
/*  62 */       return null;
     }
 
/*  65 */     int pid = -1;
     try {
/*  67 */       pid = Integer.parseInt(matcher.group(2));
     }
     catch (NumberFormatException ignored) {
     }
/*  71 */     int tid = -1;
     try
     {
/*  75 */       tid = Integer.decode(matcher.group(3)).intValue();
     }
     catch (NumberFormatException ignored) {
     }
/*  79 */     String pkgName = null;
/*  80 */     if ((device != null) && (pid != -1)) {
/*  81 */       pkgName = device.getClientName(pid);
     }
/*  83 */     if ((pkgName == null) || (pkgName.isEmpty())) {
/*  84 */       pkgName = "?";
     }
 
/*  87 */     Log.LogLevel logLevel = Log.LogLevel.getByLetterString(matcher.group(4));
/*  88 */     if ((logLevel == null) && (matcher.group(4).equals("F"))) {
/*  89 */       logLevel = Log.LogLevel.ASSERT;
     }
/*  91 */     if (logLevel == null)
     {
/*  93 */       logLevel = Log.LogLevel.WARN;
     }
 
/*  96 */     this.mPrevHeader = new LogCatHeader(logLevel, pid, tid, pkgName, matcher.group(5), LogCatTimestamp.fromString(matcher.group(1)));
 
/*  99 */     return this.mPrevHeader;
   }
 
   public List<LogCatMessage> processLogLines(String[] lines, IDevice device)
   {
/* 113 */     List messages = new ArrayList(lines.length);
 
/* 115 */     for (String line : lines) {
/* 116 */       if (!line.isEmpty())
       {
/* 120 */         if (processLogHeader(line, device) == null)
         {
/* 122 */           if (this.mPrevHeader == null) {
/* 123 */             throw new IllegalStateException("No logcat header processed yet, failed to parse line: " + line);
           }
 
/* 126 */           messages.add(new LogCatMessage(this.mPrevHeader, line));
         }
       }
     }
/* 130 */     return messages;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatMessageParser
 * JD-Core Version:    0.6.2
 */