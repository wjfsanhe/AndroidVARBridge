 package com.android.ddmlib;
 
 public class DebugPortManager
 {
/* 52 */   private static IDebugPortProvider sProvider = null;
 
   public static void setProvider(IDebugPortProvider provider)
   {
/* 60 */     sProvider = provider;
   }
 
   static IDebugPortProvider getProvider()
   {
/* 68 */     return sProvider;
   }
 
   public static abstract interface IDebugPortProvider
   {
     public static final int NO_STATIC_PORT = -1;
 
     public abstract int getPort(IDevice paramIDevice, String paramString);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.DebugPortManager
 * JD-Core Version:    0.6.2
 */