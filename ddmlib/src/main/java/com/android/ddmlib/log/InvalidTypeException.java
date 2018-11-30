 package com.android.ddmlib.log;
 
 public final class InvalidTypeException extends Exception
 {
   private static final long serialVersionUID = 1L;
 
   public InvalidTypeException()
   {
/* 36 */     super("Invalid Type");
   }
 
   public InvalidTypeException(String message)
   {
/* 46 */     super(message);
   }
 
   public InvalidTypeException(Throwable cause)
   {
/* 59 */     super(cause);
   }
 
   public InvalidTypeException(String message, Throwable cause)
   {
/* 72 */     super(message, cause);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.InvalidTypeException
 * JD-Core Version:    0.6.2
 */