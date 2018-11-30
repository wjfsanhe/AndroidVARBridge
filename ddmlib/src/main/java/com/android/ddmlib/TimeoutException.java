 package com.android.ddmlib;
 
 public class TimeoutException extends Exception
 {
   private static final long serialVersionUID = 1L;
 
   public TimeoutException()
   {
   }
 
   public TimeoutException(String s)
   {
/* 31 */     super(s);
   }
 
   public TimeoutException(String s, Throwable throwable) {
/* 35 */     super(s, throwable);
   }
 
   public TimeoutException(Throwable throwable) {
/* 39 */     super(throwable);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.TimeoutException
 * JD-Core Version:    0.6.2
 */