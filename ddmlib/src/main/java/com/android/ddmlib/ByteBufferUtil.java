 package com.android.ddmlib;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.MappedByteBuffer;
 import java.nio.channels.FileChannel;
 import java.nio.channels.FileChannel.MapMode;
 
 public class ByteBufferUtil
 {
   public static ByteBuffer mapFile(File f, long offset, ByteOrder byteOrder)
     throws IOException
   {
/* 32 */     FileInputStream dataFile = new FileInputStream(f);
     try {
/* 34 */       FileChannel fc = dataFile.getChannel();
/* 35 */       MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, f.length() - offset);
/* 36 */       buffer.order(byteOrder);
/* 37 */       return buffer;
     } finally {
/* 39 */       dataFile.close();
     }
   }
 
   public static String getString(ByteBuffer buf, int len)
   {
/* 45 */     char[] data = new char[len];
/* 46 */     for (int i = 0; i < len; i++)
/* 47 */       data[i] = buf.getChar();
/* 48 */     return new String(data);
   }
 
   public static void putString(ByteBuffer buf, String str) {
/* 52 */     int len = str.length();
/* 53 */     for (int i = 0; i < len; i++)
/* 54 */       buf.putChar(str.charAt(i));
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.ByteBufferUtil
 * JD-Core Version:    0.6.2
 */