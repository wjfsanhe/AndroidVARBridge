 package com.android.ddmlib;
 
 import java.util.concurrent.TimeUnit;
 
 public class ScreenRecorderOptions
 {
   public final int width;
   public final int height;
   public final int bitrateMbps;
   public final long timeLimit;
   public final TimeUnit timeLimitUnits;
 
   private ScreenRecorderOptions(Builder builder)
   {
/* 35 */     this.width = builder.mWidth;
/* 36 */     this.height = builder.mHeight;
 
/* 38 */     this.bitrateMbps = builder.mBitRate;
 
/* 40 */     this.timeLimit = builder.mTime;
/* 41 */     this.timeLimitUnits = builder.mTimeUnits; } 
   public static class Builder { private int mWidth;
     private int mHeight;
     private int mBitRate;
     private long mTime;
     private TimeUnit mTimeUnits;
 
/* 52 */     public Builder setSize(int w, int h) { this.mWidth = w;
/* 53 */       this.mHeight = h;
/* 54 */       return this; }
 
     public Builder setBitRate(int bitRateMbps)
     {
/* 58 */       this.mBitRate = bitRateMbps;
/* 59 */       return this;
     }
 
     public Builder setTimeLimit(long time, TimeUnit units) {
/* 63 */       this.mTime = time;
/* 64 */       this.mTimeUnits = units;
/* 65 */       return this;
     }
 
     public ScreenRecorderOptions build() {
/* 69 */       return new ScreenRecorderOptions(this);
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.ScreenRecorderOptions
 * JD-Core Version:    0.6.2
 */