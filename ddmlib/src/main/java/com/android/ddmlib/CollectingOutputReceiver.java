 package com.android.ddmlib;
 
 import com.google.common.base.Charsets;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 public class CollectingOutputReceiver
   implements IShellOutputReceiver
 {
   private CountDownLatch mCompletionLatch;
/* 30 */   private StringBuffer mOutputBuffer = new StringBuffer();
/* 31 */   private AtomicBoolean mIsCanceled = new AtomicBoolean(false);
 
   public CollectingOutputReceiver() {
   }
 
   public CollectingOutputReceiver(CountDownLatch commandCompleteLatch) {
/* 37 */     this.mCompletionLatch = commandCompleteLatch;
   }
 
   public String getOutput() {
/* 41 */     return this.mOutputBuffer.toString();
   }
 
   public boolean isCancelled()
   {
/* 46 */     return this.mIsCanceled.get();
   }
 
   public void cancel()
   {
/* 53 */     this.mIsCanceled.set(true);
   }
 
   public void addOutput(byte[] data, int offset, int length)
   {
/* 58 */     if (!isCancelled())
     {
/* 60 */       String s = new String(data, offset, length, Charsets.UTF_8);
/* 61 */       this.mOutputBuffer.append(s);
     }
   }
 
   public void flush()
   {
/* 67 */     if (this.mCompletionLatch != null)
/* 68 */       this.mCompletionLatch.countDown();
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.CollectingOutputReceiver
 * JD-Core Version:    0.6.2
 */