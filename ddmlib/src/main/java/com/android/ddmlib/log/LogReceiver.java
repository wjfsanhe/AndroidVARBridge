 package com.android.ddmlib.log;
 
 import com.android.ddmlib.utils.ArrayHelper;
 import java.security.InvalidParameterException;
 
 public final class LogReceiver
 {
   private static final int ENTRY_HEADER_SIZE = 20;
   private LogEntry mCurrentEntry;
/*  83 */   private byte[] mEntryHeaderBuffer = new byte[20];
 
/*  85 */   private int mEntryHeaderOffset = 0;
 
/*  87 */   private int mEntryDataOffset = 0;
   private ILogListener mListener;
/*  92 */   private boolean mIsCancelled = false;
 
   public LogReceiver(ILogListener listener)
   {
/* 102 */     this.mListener = listener;
   }
 
   public void parseNewData(byte[] data, int offset, int length)
   {
/* 114 */     if (this.mListener != null) {
/* 115 */       this.mListener.newData(data, offset, length);
     }
 
/* 119 */     while ((length > 0) && (!this.mIsCancelled))
     {
/* 121 */       if (this.mCurrentEntry == null) {
/* 122 */         if (this.mEntryHeaderOffset + length < 20)
         {
/* 125 */           System.arraycopy(data, offset, this.mEntryHeaderBuffer, this.mEntryHeaderOffset, length);
/* 126 */           this.mEntryHeaderOffset += length;
/* 127 */           return;
         }
 
/* 131 */         if (this.mEntryHeaderOffset != 0)
         {
/* 133 */           int size = 20 - this.mEntryHeaderOffset;
/* 134 */           System.arraycopy(data, offset, this.mEntryHeaderBuffer, this.mEntryHeaderOffset, size);
 
/* 138 */           this.mCurrentEntry = createEntry(this.mEntryHeaderBuffer, 0);
 
/* 141 */           this.mEntryHeaderOffset = 0;
 
/* 145 */           offset += size;
/* 146 */           length -= size;
         }
         else {
/* 149 */           this.mCurrentEntry = createEntry(data, offset);
 
/* 153 */           offset += 20;
/* 154 */           length -= 20;
         }
 
       }
 
/* 163 */       if (length >= this.mCurrentEntry.len - this.mEntryDataOffset)
       {
/* 166 */         int dataSize = this.mCurrentEntry.len - this.mEntryDataOffset;
 
/* 169 */         System.arraycopy(data, offset, this.mCurrentEntry.data, this.mEntryDataOffset, dataSize);
 
/* 172 */         if (this.mListener != null) {
/* 173 */           this.mListener.newEntry(this.mCurrentEntry);
         }
 
/* 178 */         this.mEntryDataOffset = 0;
/* 179 */         this.mCurrentEntry = null;
 
/* 183 */         offset += dataSize;
/* 184 */         length -= dataSize;
       }
       else
       {
/* 188 */         System.arraycopy(data, offset, this.mCurrentEntry.data, this.mEntryDataOffset, length);
 
/* 191 */         this.mEntryDataOffset += length;
/* 192 */         return;
       }
     }
   }
 
   public boolean isCancelled()
   {
/* 201 */     return this.mIsCancelled;
   }
 
   public void cancel()
   {
/* 208 */     this.mIsCancelled = true;
   }
 
   private LogEntry createEntry(byte[] data, int offset)
   {
/* 219 */     if (data.length < offset + 20) {
/* 220 */       throw new InvalidParameterException("Buffer not big enough to hold full LoggerEntry header");
     }
 
/* 225 */     LogEntry entry = new LogEntry();
/* 226 */     entry.len = ArrayHelper.swapU16bitFromArray(data, offset);
 
/* 230 */     offset += 4;
 
/* 232 */     entry.pid = ArrayHelper.swap32bitFromArray(data, offset);
/* 233 */     offset += 4;
/* 234 */     entry.tid = ArrayHelper.swap32bitFromArray(data, offset);
/* 235 */     offset += 4;
/* 236 */     entry.sec = ArrayHelper.swap32bitFromArray(data, offset);
/* 237 */     offset += 4;
/* 238 */     entry.nsec = ArrayHelper.swap32bitFromArray(data, offset);
/* 239 */     offset += 4;
 
/* 242 */     entry.data = new byte[entry.len];
 
/* 244 */     return entry;
   }
 
   public static abstract interface ILogListener
   {
     public abstract void newEntry(LogReceiver.LogEntry paramLogEntry);
 
     public abstract void newData(byte[] paramArrayOfByte, int paramInt1, int paramInt2);
   }
 
   public static final class LogEntry
   {
     public int len;
     public int pid;
     public int tid;
     public int sec;
     public int nsec;
     public byte[] data;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.LogReceiver
 * JD-Core Version:    0.6.2
 */