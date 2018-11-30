 package com.android.ddmlib;
 
 public final class NativeLibraryMapInfo
 {
   private long mStartAddr;
   private long mEndAddr;
   private String mLibrary;
 
   NativeLibraryMapInfo(long startAddr, long endAddr, String library)
   {
/* 37 */     this.mStartAddr = startAddr;
/* 38 */     this.mEndAddr = endAddr;
/* 39 */     this.mLibrary = library;
   }
 
   public String getLibraryName()
   {
/* 46 */     return this.mLibrary;
   }
 
   public long getStartAddress()
   {
/* 53 */     return this.mStartAddr;
   }
 
   public long getEndAddress()
   {
/* 60 */     return this.mEndAddr;
   }
 
   public boolean isWithinLibrary(long address)
   {
/* 71 */     return (address >= this.mStartAddr) && (address <= this.mEndAddr);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.NativeLibraryMapInfo
 * JD-Core Version:    0.6.2
 */