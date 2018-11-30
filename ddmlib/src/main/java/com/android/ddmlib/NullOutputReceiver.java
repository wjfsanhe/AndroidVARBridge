 package com.android.ddmlib;
 
 public final class NullOutputReceiver
   implements IShellOutputReceiver
 {
/* 25 */   private static NullOutputReceiver sReceiver = new NullOutputReceiver();
 
   public static IShellOutputReceiver getReceiver() {
/* 28 */     return sReceiver;
   }
 
   public void addOutput(byte[] data, int offset, int length)
   {
   }
 
   public void flush()
   {
   }
 
   public boolean isCancelled()
   {
/* 50 */     return false;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.NullOutputReceiver
 * JD-Core Version:    0.6.2
 */