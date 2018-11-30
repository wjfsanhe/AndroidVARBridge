 package com.android.ddmlib;
 
 public final class DdmConstants
 {
   public static final int PLATFORM_UNKNOWN = 0;
   public static final int PLATFORM_LINUX = 1;
   public static final int PLATFORM_WINDOWS = 2;
   public static final int PLATFORM_DARWIN = 3;
/* 30 */   public static final int CURRENT_PLATFORM = currentPlatform();
   public static final String EXTENSION = "trace";
   public static final String DOT_TRACE = ".trace";
/* 39 */   public static final String FN_HPROF_CONVERTER = CURRENT_PLATFORM == 2 ? "hprof-conv.exe" : "hprof-conv";
 
/* 43 */   public static final String FN_TRACEVIEW = CURRENT_PLATFORM == 2 ? "traceview.bat" : "traceview";
 
   public static int currentPlatform()
   {
/* 53 */     String os = System.getProperty("os.name");
/* 54 */     if (os.startsWith("Mac OS"))
/* 55 */       return 3;
/* 56 */     if (os.startsWith("Windows"))
/* 57 */       return 2;
/* 58 */     if (os.startsWith("Linux")) {
/* 59 */       return 1;
     }
 
/* 62 */     return 0;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.DdmConstants
 * JD-Core Version:    0.6.2
 */