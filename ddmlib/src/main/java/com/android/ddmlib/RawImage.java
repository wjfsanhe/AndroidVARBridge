 package com.android.ddmlib;
 
 import java.nio.ByteBuffer;
 
 public final class RawImage
 {
   public int version;
   public int bpp;
   public int size;
   public int width;
   public int height;
   public int red_offset;
   public int red_length;
   public int blue_offset;
   public int blue_length;
   public int green_offset;
   public int green_length;
   public int alpha_offset;
   public int alpha_length;
   public byte[] data;
 
   public boolean readHeader(int version, ByteBuffer buf)
   {
/*  49 */     this.version = version;
 
/*  51 */     if (version == 16)
     {
/*  53 */       this.bpp = 16;
 
/*  56 */       this.size = buf.getInt();
/*  57 */       this.width = buf.getInt();
/*  58 */       this.height = buf.getInt();
 
/*  61 */       this.red_offset = 11;
/*  62 */       this.red_length = 5;
/*  63 */       this.green_offset = 5;
/*  64 */       this.green_length = 6;
/*  65 */       this.blue_offset = 0;
/*  66 */       this.blue_length = 5;
/*  67 */       this.alpha_offset = 0;
/*  68 */       this.alpha_length = 0;
/*  69 */     } else if (version == 1) {
/*  70 */       this.bpp = buf.getInt();
/*  71 */       this.size = buf.getInt();
/*  72 */       this.width = buf.getInt();
/*  73 */       this.height = buf.getInt();
/*  74 */       this.red_offset = buf.getInt();
/*  75 */       this.red_length = buf.getInt();
/*  76 */       this.blue_offset = buf.getInt();
/*  77 */       this.blue_length = buf.getInt();
/*  78 */       this.green_offset = buf.getInt();
/*  79 */       this.green_length = buf.getInt();
/*  80 */       this.alpha_offset = buf.getInt();
/*  81 */       this.alpha_length = buf.getInt();
     }
     else {
/*  84 */       return false;
     }
 
/*  87 */     return true;
   }
 
   public int getRedMask()
   {
/*  95 */     return getMask(this.red_length, this.red_offset);
   }
 
   public int getGreenMask()
   {
/* 103 */     return getMask(this.green_length, this.green_offset);
   }
 
   public int getBlueMask()
   {
/* 111 */     return getMask(this.blue_length, this.blue_offset);
   }
 
   public static int getHeaderSize(int version)
   {
/* 120 */     switch (version) {
     case 16:
/* 122 */       return 3;
     case 1:
/* 124 */       return 12;
     }
 
/* 127 */     return 0;
   }
 
   public RawImage getRotated()
   {
/* 135 */     RawImage rotated = new RawImage();
/* 136 */     rotated.version = this.version;
/* 137 */     rotated.bpp = this.bpp;
/* 138 */     rotated.size = this.size;
/* 139 */     rotated.red_offset = this.red_offset;
/* 140 */     rotated.red_length = this.red_length;
/* 141 */     rotated.blue_offset = this.blue_offset;
/* 142 */     rotated.blue_length = this.blue_length;
/* 143 */     rotated.green_offset = this.green_offset;
/* 144 */     rotated.green_length = this.green_length;
/* 145 */     rotated.alpha_offset = this.alpha_offset;
/* 146 */     rotated.alpha_length = this.alpha_length;
 
/* 148 */     rotated.width = this.height;
/* 149 */     rotated.height = this.width;
 
/* 151 */     int count = this.data.length;
/* 152 */     rotated.data = new byte[count];
 
/* 154 */     int byteCount = this.bpp >> 3;
/* 155 */     int w = this.width;
/* 156 */     int h = this.height;
/* 157 */     for (int y = 0; y < h; y++) {
/* 158 */       for (int x = 0; x < w; x++) {
/* 159 */         System.arraycopy(this.data, (y * w + x) * byteCount, rotated.data, ((w - x - 1) * h + y) * byteCount, byteCount);
       }
 
     }
 
/* 166 */     return rotated;
   }
 
   public int getARGB(int index)
   {
     int a;
     int b;
     int g;
     int r;
     int value;
/* 175 */     if (this.bpp == 16) {
/* 176 */       value = this.data[index] & 0xFF;
/* 177 */       value |= this.data[(index + 1)] << 8 & 0xFF00;
 
/* 180 */       r = (value >>> 11 & 0x1F) * 255 / 31;
/* 181 */       g = (value >>> 5 & 0x3F) * 255 / 63;
/* 182 */       b = (value & 0x1F) * 255 / 31;
/* 183 */       a = 255;
     }
     else
     {

/* 184 */       if (this.bpp == 32) {
/* 185 */         value = this.data[index] & 0xFF;
/* 186 */         value |= (this.data[(index + 1)] & 0xFF) << 8;
/* 187 */         value |= (this.data[(index + 2)] & 0xFF) << 16;
/* 188 */         value |= (this.data[(index + 3)] & 0xFF) << 24;
/* 189 */         r = (value >>> this.red_offset & getMask(this.red_length)) << 8 - this.red_length;
/* 190 */         g = (value >>> this.green_offset & getMask(this.green_length)) << 8 - this.green_length;
/* 191 */         b = (value >>> this.blue_offset & getMask(this.blue_length)) << 8 - this.blue_length;
/* 192 */         a = (value >>> this.alpha_offset & getMask(this.alpha_length)) << 8 - this.alpha_length;
       } else {
/* 194 */         throw new UnsupportedOperationException("RawImage.getARGB(int) only works in 16 and 32 bit mode.");
       }
     }

/* 197 */     return a << 24 | r << 16 | g << 8 | b;
   }
 
   private int getMask(int length, int offset)
   {
/* 205 */     int res = getMask(length) << offset;
 
/* 208 */     if (this.bpp == 32) {
/* 209 */       return Integer.reverseBytes(res);
     }
 
/* 212 */     return res;
   }
 
   private static int getMask(int length)
   {
/* 221 */     return (1 << length) - 1;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.RawImage
 * JD-Core Version:    0.6.2
 */