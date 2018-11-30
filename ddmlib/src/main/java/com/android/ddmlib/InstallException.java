 package com.android.ddmlib;
 
 public class InstallException extends CanceledException
 {
   private static final long serialVersionUID = 1L;
 
   public InstallException(Throwable cause)
   {
/* 26 */     super(cause.getMessage(), cause);
   }
 
   public InstallException(String message) {
/* 30 */     super(message);
   }
 
   public InstallException(String message, Throwable cause) {
/* 34 */     super(message, cause);
   }
 
   public boolean wasCanceled()
   {
/* 43 */     Throwable cause = getCause();
/* 44 */     return ((cause instanceof SyncException)) && (((SyncException)cause).wasCanceled());
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.InstallException
 * JD-Core Version:    0.6.2
 */