 package com.android.ddmlib;
 
 public class AdbCommandRejectedException extends Exception
 {
   private static final long serialVersionUID = 1L;
   private final boolean mIsDeviceOffline;
   private final boolean mErrorDuringDeviceSelection;
 
   AdbCommandRejectedException(String message)
   {
/* 29 */     super(message);
/* 30 */     this.mIsDeviceOffline = "device offline".equals(message);
/* 31 */     this.mErrorDuringDeviceSelection = false;
   }
 
   AdbCommandRejectedException(String message, boolean errorDuringDeviceSelection) {
/* 35 */     super(message);
/* 36 */     this.mErrorDuringDeviceSelection = errorDuringDeviceSelection;
/* 37 */     this.mIsDeviceOffline = "device offline".equals(message);
   }
 
   public boolean isDeviceOffline()
   {
/* 44 */     return this.mIsDeviceOffline;
   }
 
   public boolean wasErrorDuringDeviceSelection()
   {
/* 53 */     return this.mErrorDuringDeviceSelection;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.AdbCommandRejectedException
 * JD-Core Version:    0.6.2
 */