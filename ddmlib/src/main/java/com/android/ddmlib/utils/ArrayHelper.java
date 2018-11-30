 package com.android.ddmlib.utils;
 
 public final class ArrayHelper
 {
   public static void swap32bitsToArray(int value, byte[] dest, int offset)
   {
/* 32 */     dest[offset] = ((byte)(value & 0xFF));
/* 33 */     dest[(offset + 1)] = ((byte)((value & 0xFF00) >> 8));
/* 34 */     dest[(offset + 2)] = ((byte)((value & 0xFF0000) >> 16));
/* 35 */     dest[(offset + 3)] = ((byte)((value & 0xFF000000) >> 24));
   }
 
   public static int swap32bitFromArray(byte[] value, int offset)
   {
/* 45 */     int v = 0;
/* 46 */     v |= value[offset] & 0xFF;
/* 47 */     v |= (value[(offset + 1)] & 0xFF) << 8;
/* 48 */     v |= (value[(offset + 2)] & 0xFF) << 16;
/* 49 */     v |= (value[(offset + 3)] & 0xFF) << 24;
 
/* 51 */     return v;
   }
 
   public static int swapU16bitFromArray(byte[] value, int offset)
   {
/* 63 */     int v = 0;
/* 64 */     v |= value[offset] & 0xFF;
/* 65 */     v |= (value[(offset + 1)] & 0xFF) << 8;
 
/* 67 */     return v;
   }
 
   public static long swap64bitFromArray(byte[] value, int offset)
   {
/* 78 */     long v = 0L;
/* 79 */     v |= value[offset] & 0xFF;
/* 80 */     v |= (value[(offset + 1)] & 0xFF) << 8;
/* 81 */     v |= (value[(offset + 2)] & 0xFF) << 16;
/* 82 */     v |= (value[(offset + 3)] & 0xFF) << 24;
/* 83 */     v |= (value[(offset + 4)] & 0xFF) << 32;
/* 84 */     v |= (value[(offset + 5)] & 0xFF) << 40;
/* 85 */     v |= (value[(offset + 6)] & 0xFF) << 48;
/* 86 */     v |= (value[(offset + 7)] & 0xFF) << 56;
 
/* 88 */     return v;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.utils.ArrayHelper
 * JD-Core Version:    0.6.2
 */