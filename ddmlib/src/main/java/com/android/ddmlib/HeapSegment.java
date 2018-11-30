 package com.android.ddmlib;
 
 import java.nio.BufferUnderflowException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.text.ParseException;
 
 public final class HeapSegment
   implements Comparable<HeapSegment>
 {
   protected int mHeapId;
   protected int mAllocationUnitSize;
   protected long mStartAddress;
   protected int mOffset;
   protected int mAllocationUnitCount;
   protected ByteBuffer mUsageData;
   private static final long INVALID_START_ADDRESS = -1L;
 
   public HeapSegment(ByteBuffer hpsgData)
     throws BufferUnderflowException
   {
/* 255 */     hpsgData.order(ByteOrder.BIG_ENDIAN);
/* 256 */     this.mHeapId = hpsgData.getInt();
/* 257 */     this.mAllocationUnitSize = hpsgData.get();
/* 258 */     this.mStartAddress = (hpsgData.getInt() & 0xFFFFFFFF);
/* 259 */     this.mOffset = hpsgData.getInt();
/* 260 */     this.mAllocationUnitCount = hpsgData.getInt();
 
/* 263 */     this.mUsageData = hpsgData.slice();
/* 264 */     this.mUsageData.order(ByteOrder.BIG_ENDIAN);
   }
 
   public boolean isValid()
   {
/* 280 */     return this.mStartAddress != -1L;
   }
 
   public boolean canAppend(HeapSegment other)
   {
/* 291 */     return (isValid()) && (other.isValid()) && (this.mHeapId == other.mHeapId) && (this.mAllocationUnitSize == other.mAllocationUnitSize) && (getEndAddress() == other.getStartAddress());
   }
 
   public boolean append(HeapSegment other)
   {
/* 307 */     if (canAppend(other))
     {
/* 311 */       int pos = this.mUsageData.position();
 
/* 314 */       if (this.mUsageData.capacity() - this.mUsageData.limit() < other.mUsageData.limit())
       {
/* 319 */         int newSize = this.mUsageData.limit() + other.mUsageData.limit();
/* 320 */         ByteBuffer newData = ByteBuffer.allocate(newSize * 2);
 
/* 322 */         this.mUsageData.rewind();
/* 323 */         newData.put(this.mUsageData);
/* 324 */         this.mUsageData = newData;
       }
 
/* 328 */       other.mUsageData.rewind();
/* 329 */       this.mUsageData.put(other.mUsageData);
/* 330 */       this.mUsageData.position(pos);
 
/* 333 */       this.mAllocationUnitCount += other.mAllocationUnitCount;
 
/* 336 */       other.mStartAddress = -1L;
/* 337 */       other.mUsageData = null;
 
/* 339 */       return true;
     }
/* 341 */     return false;
   }
 
   public long getStartAddress()
   {
/* 346 */     return this.mStartAddress + this.mOffset;
   }
 
   public int getLength() {
/* 350 */     return this.mAllocationUnitSize * this.mAllocationUnitCount;
   }
 
   public long getEndAddress() {
/* 354 */     return getStartAddress() + getLength();
   }
 
   public void rewindElements() {
/* 358 */     if (this.mUsageData != null)
/* 359 */       this.mUsageData.rewind();
   }
 
   public HeapSegmentElement getNextElement(HeapSegmentElement reuse)
   {
     try {
/* 365 */       if (reuse != null) {
/* 366 */         return reuse.set(this);
       }
/* 368 */       return new HeapSegmentElement(this);
     }
     catch (BufferUnderflowException ex)
     {
     }
     catch (ParseException ex)
     {
     }
 
/* 378 */     return null;
   }
 
   public boolean equals(Object o)
   {
/* 386 */     if ((o instanceof HeapSegment)) {
/* 387 */       return compareTo((HeapSegment)o) == 0;
     }
/* 389 */     return false;
   }
 
   public int hashCode()
   {
/* 394 */     return this.mHeapId * 31 + this.mAllocationUnitSize * 31 + (int)this.mStartAddress * 31 + this.mOffset * 31 + this.mAllocationUnitCount * 31 + this.mUsageData.hashCode();
   }
 
   public String toString()
   {
/* 404 */     StringBuilder str = new StringBuilder();
 
/* 406 */     str.append("HeapSegment { heap ").append(this.mHeapId).append(", start 0x").append(Integer.toHexString((int)getStartAddress())).append(", length ").append(getLength()).append(" }");
 
/* 412 */     return str.toString();
   }
 
   public int compareTo(HeapSegment other)
   {
/* 417 */     if (this.mHeapId != other.mHeapId) {
/* 418 */       return this.mHeapId < other.mHeapId ? -1 : 1;
     }
/* 420 */     if (getStartAddress() != other.getStartAddress()) {
/* 421 */       return getStartAddress() < other.getStartAddress() ? -1 : 1;
     }
 
/* 431 */     if (this.mAllocationUnitSize != other.mAllocationUnitSize) {
/* 432 */       return this.mAllocationUnitSize < other.mAllocationUnitSize ? -1 : 1;
     }
/* 434 */     if (this.mStartAddress != other.mStartAddress) {
/* 435 */       return this.mStartAddress < other.mStartAddress ? -1 : 1;
     }
/* 437 */     if (this.mOffset != other.mOffset) {
/* 438 */       return this.mOffset < other.mOffset ? -1 : 1;
     }
/* 440 */     if (this.mAllocationUnitCount != other.mAllocationUnitCount) {
/* 441 */       return this.mAllocationUnitCount < other.mAllocationUnitCount ? -1 : 1;
     }
/* 443 */     if (this.mUsageData != other.mUsageData) {
/* 444 */       return this.mUsageData.compareTo(other.mUsageData);
     }
/* 446 */     return 0;
   }
 
   public static class HeapSegmentElement
     implements Comparable<HeapSegmentElement>
   {
     public static final int SOLIDITY_FREE = 0;
     public static final int SOLIDITY_HARD = 1;
     public static final int SOLIDITY_SOFT = 2;
     public static final int SOLIDITY_WEAK = 3;
     public static final int SOLIDITY_PHANTOM = 4;
     public static final int SOLIDITY_FINALIZABLE = 5;
     public static final int SOLIDITY_SWEEP = 6;
     public static final int SOLIDITY_INVALID = -1;
     public static final int KIND_OBJECT = 0;
     public static final int KIND_CLASS_OBJECT = 1;
     public static final int KIND_ARRAY_1 = 2;
     public static final int KIND_ARRAY_2 = 3;
     public static final int KIND_ARRAY_4 = 4;
     public static final int KIND_ARRAY_8 = 5;
     public static final int KIND_UNKNOWN = 6;
     public static final int KIND_NATIVE = 7;
     public static final int KIND_INVALID = -1;
     private static final int PARTIAL_MASK = 128;
     private int mSolidity;
     private int mKind;
     private int mLength;
 
     public HeapSegmentElement()
     {
/* 127 */       setSolidity(-1);
/* 128 */       setKind(-1);
/* 129 */       setLength(-1);
     }
 
     public HeapSegmentElement(HeapSegment hs)
       throws BufferUnderflowException, ParseException
     {
/* 144 */       set(hs);
     }
 
     public HeapSegmentElement set(HeapSegment hs)
       throws BufferUnderflowException, ParseException
     {
/* 164 */       ByteBuffer data = hs.mUsageData;
/* 165 */       int eState = data.get() & 0xFF;
/* 166 */       int eLen = (data.get() & 0xFF) + 1;
 
/* 168 */       while ((eState & 0x80) != 0)
       {
/* 173 */         int nextState = data.get() & 0xFF;
/* 174 */         if ((nextState & 0xFFFFFF7F) != (eState & 0xFFFFFF7F)) {
/* 175 */           throw new ParseException("State mismatch", data.position());
         }
/* 177 */         eState = nextState;
/* 178 */         eLen += (data.get() & 0xFF) + 1;
       }
 
/* 181 */       setSolidity(eState & 0x7);
/* 182 */       setKind(eState >> 3 & 0x7);
/* 183 */       setLength(eLen * hs.mAllocationUnitSize);
 
/* 185 */       return this;
     }
 
     public int getSolidity() {
/* 189 */       return this.mSolidity;
     }
 
     public void setSolidity(int solidity) {
/* 193 */       this.mSolidity = solidity;
     }
 
     public int getKind() {
/* 197 */       return this.mKind;
     }
 
     public void setKind(int kind) {
/* 201 */       this.mKind = kind;
     }
 
     public int getLength() {
/* 205 */       return this.mLength;
     }
 
     public void setLength(int length) {
/* 209 */       this.mLength = length;
     }
 
     public int compareTo(HeapSegmentElement other)
     {
/* 214 */       if (this.mLength != other.mLength) {
/* 215 */         return this.mLength < other.mLength ? -1 : 1;
       }
/* 217 */       return 0;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HeapSegment
 * JD-Core Version:    0.6.2
 */