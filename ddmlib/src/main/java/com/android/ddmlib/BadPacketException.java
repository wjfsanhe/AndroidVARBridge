 package com.android.ddmlib;
 
 class BadPacketException extends RuntimeException
 {
   public BadPacketException()
   {
   }
 
   public BadPacketException(String msg)
   {
/* 32 */     super(msg);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.BadPacketException
 * JD-Core Version:    0.6.2
 */