 package com.android.ddmlib;
 
 import com.google.common.collect.ImmutableMap;
 import java.awt.Dimension;
 import java.awt.image.BufferedImage;
 import java.util.Map;
 
 public class BitmapDecoder
 {
   public static final String BITMAP_FQCN = "android.graphics.Bitmap";
   public static final String BITMAP_DRAWABLE_FQCN = "android.graphics.drawable.BitmapDrawable";
/*  49 */   protected static final Map<String, BitmapExtractor> SUPPORTED_FORMATS = ImmutableMap.of("\"ARGB_8888\"", new ARGB8888_BitmapExtractor(), "\"RGB_565\"", new RGB565_BitmapExtractor(), "\"ALPHA_8\"", new ALPHA8_BitmapExtractor());
   private static final int MAX_DIMENSION = 1024;
 
   public static BufferedImage getBitmap(BitmapDataProvider dataProvider)
     throws Exception
   {
/*  62 */     String config = dataProvider.getBitmapConfigName();
/*  63 */     if (config == null) {
/*  64 */       throw new RuntimeException("Unable to determine bitmap configuration");
     }
 
/*  67 */     BitmapExtractor bitmapExtractor = (BitmapExtractor)SUPPORTED_FORMATS.get(config);
/*  68 */     if (bitmapExtractor == null) {
/*  69 */       throw new RuntimeException("Unsupported bitmap configuration: " + config);
     }
 
/*  72 */     Dimension size = dataProvider.getDimension();
/*  73 */     if (size == null) {
/*  74 */       throw new RuntimeException("Unable to determine image dimensions.");
     }
 
/*  78 */     if ((size.width > 1024) || (size.height > 1024)) {
/*  79 */       boolean couldDownsize = dataProvider.downsizeBitmap(size);
/*  80 */       if (!couldDownsize) {
/*  81 */         throw new RuntimeException("Unable to create scaled bitmap");
       }
 
/*  84 */       size = dataProvider.getDimension();
/*  85 */       if (size == null) {
/*  86 */         throw new RuntimeException("Unable to obtained scaled bitmap's dimensions");
       }
     }
 
/*  90 */     return bitmapExtractor.getImage(size.width, size.height, dataProvider.getPixelBytes(size));
   }
 
   private static class ALPHA8_BitmapExtractor
     implements BitmapDecoder.BitmapExtractor
   {
     public BufferedImage getImage(int width, int height, byte[] rgb)
     {
/* 149 */       int bytesPerPixel = 1;
 
/* 152 */       BufferedImage bufferedImage = new BufferedImage(width, height, 2);
 
/* 155 */       for (int y = 0; y < height; y++) {
/* 156 */         int stride = y * width;
/* 157 */         for (int x = 0; x < width; x++) {
/* 158 */           int index = stride + x;
/* 159 */           int value = rgb[index];
/* 160 */           int rgba = value << 24 | 0xFF0000 | 0xFF00 | 0xFF;
/* 161 */           bufferedImage.setRGB(x, y, rgba);
         }
       }
 
/* 165 */       return bufferedImage;
     }
   }
 
   private static class RGB565_BitmapExtractor
     implements BitmapDecoder.BitmapExtractor
   {
     public BufferedImage getImage(int width, int height, byte[] rgb)
     {
/* 120 */       int bytesPerPixel = 2;
 
/* 123 */       BufferedImage bufferedImage = new BufferedImage(width, height, 2);
 
/* 126 */       for (int y = 0; y < height; y++) {
/* 127 */         int stride = y * width;
/* 128 */         for (int x = 0; x < width; x++) {
/* 129 */           int index = (stride + x) * bytesPerPixel;
/* 130 */           int value = rgb[index] & 0xFF | rgb[(index + 1)] << 8 & 0xFF00;
 
/* 133 */           int r = (value >>> 11 & 0x1F) * 255 / 31;
/* 134 */           int g = (value >>> 5 & 0x3F) * 255 / 63;
/* 135 */           int b = (value & 0x1F) * 255 / 31;
/* 136 */           int a = 255;
/* 137 */           int rgba = a << 24 | r << 16 | g << 8 | b;
/* 138 */           bufferedImage.setRGB(x, y, rgba);
         }
       }
 
/* 142 */       return bufferedImage;
     }
   }
 
   private static class ARGB8888_BitmapExtractor
     implements BitmapDecoder.BitmapExtractor
   {
     public BufferedImage getImage(int width, int height, byte[] rgba)
     {
/*  97 */       BufferedImage bufferedImage = new BufferedImage(width, height, 2);
 
/* 100 */       for (int y = 0; y < height; y++) {
/* 101 */         int stride = y * width;
/* 102 */         for (int x = 0; x < width; x++) {
/* 103 */           int i = (stride + x) * 4;
/* 104 */           long rgb = 0L;
/* 105 */           rgb |= (rgba[i] & 0xFF) << 16;
/* 106 */           rgb |= (rgba[(i + 1)] & 0xFF) << 8;
/* 107 */           rgb |= rgba[(i + 2)] & 0xFF;
/* 108 */           rgb |= (rgba[(i + 3)] & 0xFF) << 24;
/* 109 */           bufferedImage.setRGB(x, y, (int)(rgb & 0xFFFFFFFF));
         }
       }
 
/* 113 */       return bufferedImage;
     }
   }
 
   private static abstract interface BitmapExtractor
   {
     public abstract BufferedImage getImage(int paramInt1, int paramInt2, byte[] paramArrayOfByte);
   }
 
   public static abstract interface BitmapDataProvider
   {
     public abstract String getBitmapConfigName()
       throws Exception;
 
     public abstract Dimension getDimension()
       throws Exception;
 
     public abstract boolean downsizeBitmap(Dimension paramDimension)
       throws Exception;
 
     public abstract byte[] getPixelBytes(Dimension paramDimension)
       throws Exception;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.BitmapDecoder
 * JD-Core Version:    0.6.2
 */