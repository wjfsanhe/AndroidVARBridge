 package com.android.ddmlib;
 
 public final class DdmPreferences
 {
   public static final boolean DEFAULT_INITIAL_THREAD_UPDATE = false;
   public static final boolean DEFAULT_INITIAL_HEAP_UPDATE = false;
   public static final int DEFAULT_SELECTED_DEBUG_PORT = 8700;
   public static final int DEFAULT_DEBUG_PORT_BASE = 8600;
/*  42 */   public static final Log.LogLevel DEFAULT_LOG_LEVEL = Log.LogLevel.VERBOSE;
   public static final int DEFAULT_TIMEOUT = 5000;
   public static final int DEFAULT_PROFILER_BUFFER_SIZE_MB = 8;
   public static final boolean DEFAULT_USE_ADBHOST = false;
   public static final String DEFAULT_ADBHOST_VALUE = "127.0.0.1";
/*  51 */   private static boolean sThreadUpdate = false;
/*  52 */   private static boolean sInitialHeapUpdate = false;
 
/*  54 */   private static int sSelectedDebugPort = 8700;
/*  55 */   private static int sDebugPortBase = 8600;
/*  56 */   private static Log.LogLevel sLogLevel = DEFAULT_LOG_LEVEL;
/*  57 */   private static int sTimeOut = 5000;
/*  58 */   private static int sProfilerBufferSizeMb = 8;
 
/*  60 */   private static boolean sUseAdbHost = false;
/*  61 */   private static String sAdbHostValue = "127.0.0.1";
 
   public static boolean getInitialThreadUpdate()
   {
/*  68 */     return sThreadUpdate;
   }
 
   public static void setInitialThreadUpdate(boolean state)
   {
/*  76 */     sThreadUpdate = state;
   }
 
   public static boolean getInitialHeapUpdate()
   {
/*  84 */     return sInitialHeapUpdate;
   }
 
   public static void setInitialHeapUpdate(boolean state)
   {
/*  94 */     sInitialHeapUpdate = state;
   }
 
   public static int getSelectedDebugPort()
   {
/* 101 */     return sSelectedDebugPort;
   }
 
   public static void setSelectedDebugPort(int port)
   {
/* 110 */     sSelectedDebugPort = port;
 
/* 112 */     MonitorThread monitorThread = MonitorThread.getInstance();
/* 113 */     if (monitorThread != null)
/* 114 */       monitorThread.setDebugSelectedPort(port);
   }
 
   public static int getDebugPortBase()
   {
/* 123 */     return sDebugPortBase;
   }
 
   public static void setDebugPortBase(int port)
   {
/* 133 */     sDebugPortBase = port;
   }
 
   public static Log.LogLevel getLogLevel()
   {
/* 140 */     return sLogLevel;
   }
 
   public static void setLogLevel(String value)
   {
/* 148 */     sLogLevel = Log.LogLevel.getByString(value);
 
/* 150 */     Log.setLevel(sLogLevel);
   }
 
   public static int getTimeOut()
   {
/* 157 */     return sTimeOut;
   }
 
   public static void setTimeOut(int timeOut)
   {
/* 166 */     sTimeOut = timeOut;
   }
 
   public static int getProfilerBufferSizeMb()
   {
/* 173 */     return sProfilerBufferSizeMb;
   }
 
   public static void setProfilerBufferSizeMb(int bufferSizeMb)
   {
/* 181 */     sProfilerBufferSizeMb = bufferSizeMb;
   }
 
   public static boolean getUseAdbHost()
   {
/* 188 */     return sUseAdbHost;
   }
 
   public static void setUseAdbHost(boolean useAdbHost)
   {
/* 196 */     sUseAdbHost = useAdbHost;
   }
 
   public static String getAdbHostValue()
   {
/* 203 */     return sAdbHostValue;
   }
 
   public static void setAdbHostValue(String adbHostValue)
   {
/* 211 */     sAdbHostValue = adbHostValue;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.DdmPreferences
 * JD-Core Version:    0.6.2
 */