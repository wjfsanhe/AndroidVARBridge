 package com.android.ddmlib;
 
 import com.google.common.base.Charsets;
 import java.util.ArrayList;
 
 public abstract class MultiLineReceiver
   implements IShellOutputReceiver
 {
/*  32 */   private boolean mTrimLines = true;
 
/*  35 */   private String mUnfinishedLine = null;
 
/*  37 */   private final ArrayList<String> mArray = new ArrayList();
 
   public void setTrimLine(boolean trim)
   {
/*  44 */     this.mTrimLines = trim;
   }
 
   public final void addOutput(byte[] data, int offset, int length)
   {
/*  53 */     if (!isCancelled()) {
/*  54 */       String s = new String(data, offset, length, Charsets.UTF_8);
 
/*  58 */       if (this.mUnfinishedLine != null) {
/*  59 */         s = this.mUnfinishedLine + s;
/*  60 */         this.mUnfinishedLine = null;
       }
 
/*  64 */       this.mArray.clear();
/*  65 */       int start = 0;
       while (true) {
/*  67 */         int index = s.indexOf(10, start);
 
/*  71 */         if (index == -1) {
/*  72 */           this.mUnfinishedLine = s.substring(start);
/*  73 */           break;
         }
 
/*  77 */         int newlineLength = 1;
/*  78 */         if ((index > 0) && (s.charAt(index - 1) == '\r')) {
/*  79 */           index--;
/*  80 */           newlineLength = 2;
         }
 
/*  84 */         String line = s.substring(start, index);
/*  85 */         if (this.mTrimLines) {
/*  86 */           line = line.trim();
         }
/*  88 */         this.mArray.add(line);
 
/*  91 */         start = index + newlineLength;
       }
 
/*  94 */       if (!this.mArray.isEmpty())
       {
/*  97 */         String[] lines = (String[])this.mArray.toArray(new String[this.mArray.size()]);
 
/* 100 */         processNewLines(lines);
       }
     }
   }
 
   public final void flush()
   {
/* 110 */     if (this.mUnfinishedLine != null) {
/* 111 */       processNewLines(new String[] { this.mUnfinishedLine });
     }
 
/* 114 */     done();
   }
 
   public void done()
   {
   }
 
   public abstract void processNewLines(String[] paramArrayOfString);
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.MultiLineReceiver
 * JD-Core Version:    0.6.2
 */