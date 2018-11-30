 package com.android.ddmlib.log;
 
 public final class InvalidValueTypeException extends Exception
 {
   private static final long serialVersionUID = 1L;
 
   public InvalidValueTypeException()
   {
/* 40 */     super("Invalid Type");
   }
 
   public InvalidValueTypeException(String message)
   {
/* 50 */     super(message);
   }
 
   public InvalidValueTypeException(Throwable cause)
   {
/* 63 */     super(cause);
   }
 
   public InvalidValueTypeException(String message, Throwable cause)
   {
/* 76 */     super(message, cause);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.InvalidValueTypeException
 * JD-Core Version:    0.6.2
 */