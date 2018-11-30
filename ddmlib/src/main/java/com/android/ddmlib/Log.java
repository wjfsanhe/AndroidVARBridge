 package com.android.ddmlib;
 
 import com.google.common.collect.Sets;
 import java.io.PrintStream;
 import java.io.PrintWriter;
 import java.io.StringWriter;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Locale;
 import java.util.Set;
 
 public final class Log
 {
/* 144 */   private static LogLevel sLevel = DdmPreferences.getLogLevel();
   private static ILogOutput sLogOutput;
/* 150 */   private static final Set<ILogOutput> sOutputLoggers = Sets.newHashSet();
 
/* 152 */   private static final char[] mSpaceLine = new char[72];
/* 153 */   private static final char[] mHexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
 
   public static void v(String tag, String message)
   {
/* 177 */     println(LogLevel.VERBOSE, tag, message);
   }
 
   public static void d(String tag, String message)
   {
/* 186 */     println(LogLevel.DEBUG, tag, message);
   }
 
   public static void i(String tag, String message)
   {
/* 195 */     println(LogLevel.INFO, tag, message);
   }
 
   public static void w(String tag, String message)
   {
/* 204 */     println(LogLevel.WARN, tag, message);
   }
 
   public static void e(String tag, String message)
   {
/* 213 */     println(LogLevel.ERROR, tag, message);
   }
 
   public static void logAndDisplay(LogLevel logLevel, String tag, String message)
   {
/* 222 */     if (!sOutputLoggers.isEmpty()) {
/* 223 */       for (ILogOutput logger : sOutputLoggers) {
/* 224 */         logger.printAndPromptLog(logLevel, tag, message);
       }
     }
 
/* 228 */     if (sLogOutput != null)
/* 229 */       sLogOutput.printAndPromptLog(logLevel, tag, message);
/* 230 */     else if (sOutputLoggers.isEmpty())
/* 231 */       println(logLevel, tag, message);
   }
 
   public static void e(String tag, Throwable throwable)
   {
/* 241 */     if (throwable != null) {
/* 242 */       StringWriter sw = new StringWriter();
/* 243 */       PrintWriter pw = new PrintWriter(sw);
 
/* 245 */       throwable.printStackTrace(pw);
/* 246 */       println(LogLevel.ERROR, tag, throwable.getMessage() + '\n' + sw.toString());
     }
   }
 
   static void setLevel(LogLevel logLevel) {
/* 251 */     sLevel = logLevel;
   }
 
   @Deprecated
   public static void setLogOutput(ILogOutput logOutput)
   {
/* 262 */     sLogOutput = logOutput;
   }
 
   public static void addLogger(ILogOutput logOutput) {
/* 266 */     sOutputLoggers.add(logOutput);
   }
 
   public static void removeLogger(ILogOutput logOutput) {
/* 270 */     sOutputLoggers.remove(logOutput);
   }
 
   static void hexDump(String tag, LogLevel level, byte[] data, int offset, int length)
   {
/* 283 */     int kHexOffset = 6;
/* 284 */     int kAscOffset = 55;
/* 285 */     char[] line = new char[mSpaceLine.length];
 
/* 288 */     boolean needErase = true;
 
/* 292 */     int baseAddr = 0;
/* 293 */     while (length != 0)
     {

       int count;
/* 294 */       if (length > 16)
       {
/* 296 */         count = 16;
       }
       else {
/* 299 */         count = length;
/* 300 */         needErase = true;
       }
 
/* 303 */       if (needErase) {
/* 304 */         System.arraycopy(mSpaceLine, 0, line, 0, mSpaceLine.length);
/* 305 */         needErase = false;
       }
 
/* 309 */       int addr = baseAddr;
/* 310 */       addr &= 65535;
/* 311 */       int ch = 3;
/* 312 */       while (addr != 0) {
/* 313 */         line[ch] = mHexDigit[(addr & 0xF)];
/* 314 */         ch--;
/* 315 */         addr >>>= 4;
       }
 
/* 319 */       ch = kHexOffset;
/* 320 */       for (int i = 0; i < count; i++) {
/* 321 */         byte val = data[(offset + i)];
 
/* 323 */         line[(ch++)] = mHexDigit[(val >>> 4 & 0xF)];
/* 324 */         line[(ch++)] = mHexDigit[(val & 0xF)];
/* 325 */         ch++;
 
/* 327 */         if ((val >= 32) && (val < 127))
/* 328 */           line[(kAscOffset + i)] = ((char)val);
         else {
/* 330 */           line[(kAscOffset + i)] = '.';
         }
       }
/* 333 */       println(level, tag, new String(line));
 
/* 336 */       length -= count;
/* 337 */       offset += count;
/* 338 */       baseAddr += count;
     }
   }
 
   static void hexDump(byte[] data)
   {
/* 347 */     hexDump("ddms", LogLevel.DEBUG, data, 0, data.length);
   }
 
   private static void println(LogLevel logLevel, String tag, String message) {
/* 351 */     if (logLevel.getPriority() < sLevel.getPriority()) {
/* 352 */       return;
     }
 
/* 355 */     if (!sOutputLoggers.isEmpty()) {
/* 356 */       for (ILogOutput logger : sOutputLoggers) {
/* 357 */         logger.printLog(logLevel, tag, message);
       }
     }
 
/* 361 */     if (sLogOutput != null)
/* 362 */       sLogOutput.printLog(logLevel, tag, message);
/* 363 */     else if (sOutputLoggers.isEmpty())
/* 364 */       printLog(logLevel, tag, message);
   }
 
   public static void printLog(LogLevel logLevel, String tag, String message)
   {
/* 369 */     System.out.print(getLogFormatString(logLevel, tag, message));

   }
 
   public static String getLogFormatString(LogLevel logLevel, String tag, String message) {
/* 373 */     SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
/* 374 */     return String.format("%s %c/%s: %s\n", new Object[] { formatter.format(new Date()), Character.valueOf(logLevel.getPriorityLetter()), tag, message });
   }
 
   static
   {
/* 157 */     int i = mSpaceLine.length - 1;
/* 158 */     while (i >= 0)
/* 159 */       mSpaceLine[(i--)] = ' ';
     char tmp162_161 = (mSpaceLine[2] = mSpaceLine[3] = 48); mSpaceLine[1] = tmp162_161; mSpaceLine[0] = tmp162_161;
/* 161 */     mSpaceLine[4] = '-';
   }
 
   static final class Config
   {
     static final boolean LOGV = true;
     static final boolean LOGD = true;
   }
 
   public static abstract interface ILogOutput
   {
     public abstract void printLog(Log.LogLevel paramLogLevel, String paramString1, String paramString2);
 
     public abstract void printAndPromptLog(Log.LogLevel paramLogLevel, String paramString1, String paramString2);
   }
 
   public static enum LogLevel
   {
/*  41 */     VERBOSE(2, "verbose", 'V'), 
/*  42 */     DEBUG(3, "debug", 'D'), 
/*  43 */     INFO(4, "info", 'I'), 
/*  44 */     WARN(5, "warn", 'W'), 
/*  45 */     ERROR(6, "error", 'E'), 
/*  46 */     ASSERT(7, "assert", 'A');
 
     private int mPriorityLevel;
     private String mStringValue;
     private char mPriorityLetter;
 
/*  53 */     private LogLevel(int intPriority, String stringValue, char priorityChar) { this.mPriorityLevel = intPriority;
/*  54 */       this.mStringValue = stringValue;
/*  55 */       this.mPriorityLetter = priorityChar; }
 
     public static LogLevel getByString(String value)
     {
/*  59 */       for (LogLevel mode : values()) {
/*  60 */         if (mode.mStringValue.equals(value)) {
/*  61 */           return mode;
         }
       }
 
/*  65 */       return null;
     }
 
     public static LogLevel getByLetter(char letter)
     {
/*  74 */       for (LogLevel mode : values()) {
/*  75 */         if (mode.mPriorityLetter == letter) {
/*  76 */           return mode;
         }
       }
 
/*  80 */       return null;
     }
 
     public static LogLevel getByLetterString(String letter)
     {
/*  92 */       if (!letter.isEmpty()) {
/*  93 */         return getByLetter(letter.charAt(0));
       }
 
/*  96 */       return null;
     }
 
     public char getPriorityLetter()
     {
/* 103 */       return this.mPriorityLetter;
     }
 
     public int getPriority()
     {
/* 110 */       return this.mPriorityLevel;
     }
 
     public String getStringValue()
     {
/* 117 */       return this.mStringValue;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.Log
 * JD-Core Version:    0.6.2
 */