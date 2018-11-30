 package com.android.ddmlib;
 
 public abstract class CanceledException extends Exception
 {
   private static final long serialVersionUID = 1L;
 
   CanceledException(String message)
   {
/* 29 */     super(message);
   }
 
   CanceledException(String message, Throwable cause) {
/* 33 */     super(message, cause);
   }
 
   public abstract boolean wasCanceled();
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.CanceledException
 * JD-Core Version:    0.6.2
 */