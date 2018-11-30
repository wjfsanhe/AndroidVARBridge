 package com.android.ddmlib.log;
 
 final class GcEventContainer extends EventContainer
 {
   public static final int GC_EVENT_TAG = 20001;
   private String processId;
   private long gcTime;
   private long bytesFreed;
   private long objectsFreed;
   private long actualSize;
   private long allowedSize;
   private long softLimit;
   private long objectsAllocated;
   private long bytesAllocated;
   private long zActualSize;
   private long zAllowedSize;
   private long zObjectsAllocated;
   private long zBytesAllocated;
   private long dlmallocFootprint;
   private long mallinfoTotalAllocatedSpace;
   private long externalLimit;
   private long externalBytesAllocated;
 
   GcEventContainer(LogReceiver.LogEntry entry, int tag, Object data)
   {
/*  52 */     super(entry, tag, data);
/*  53 */     init(data);
   }
 
   GcEventContainer(int tag, int pid, int tid, int sec, int nsec, Object data) {
/*  57 */     super(tag, pid, tid, sec, nsec, data);
/*  58 */     init(data);
   }
 
   private void init(Object data)
   {
/*  65 */     if ((data instanceof Object[])) {
/*  66 */       Object[] values = (Object[])data;
/*  67 */       for (int i = 0; i < values.length; i++)
/*  68 */         if ((values[i] instanceof Long))
/*  69 */           parseDvmHeapInfo(((Long)values[i]).longValue(), i);
     }
   }
 
   public EventContainer.EventValueType getType()
   {
/*  77 */     return EventContainer.EventValueType.LIST;
   }
 
   public boolean testValue(int index, Object value, EventContainer.CompareMethod compareMethod)
     throws InvalidTypeException
   {
/*  84 */     if (index == 0) {
/*  85 */       if (!(value instanceof String))
/*  86 */         throw new InvalidTypeException();
     }
/*  88 */     else if (!(value instanceof Long)) {
/*  89 */       throw new InvalidTypeException();
     }
 
/*  92 */     switch (compareMethod.ordinal()) {
     case 1:
/*  94 */       if (index == 0) {
/*  95 */         return this.processId.equals(value);
       }
/*  97 */       return getValueAsLong(index) == ((Long)value).longValue();
     case 2:
/* 100 */       return getValueAsLong(index) <= ((Long)value).longValue();
     case 3:
/* 102 */       return getValueAsLong(index) < ((Long)value).longValue();
     case 4:
/* 104 */       return getValueAsLong(index) >= ((Long)value).longValue();
     case 5:
/* 106 */       return getValueAsLong(index) > ((Long)value).longValue();
     case 6:
/* 108 */       return (getValueAsLong(index) & ((Long)value).longValue()) != 0L;
     }
 
/* 111 */     throw new ArrayIndexOutOfBoundsException();
   }
 
   public Object getValue(int valueIndex)
   {
/* 116 */     if (valueIndex == 0) {
/* 117 */       return this.processId;
     }
     try
     {
/* 121 */       return Long.valueOf(getValueAsLong(valueIndex));
     }
     catch (InvalidTypeException e)
     {
     }
/* 126 */     return null;
   }
 
   public double getValueAsDouble(int valueIndex) throws InvalidTypeException
   {
/* 131 */     return getValueAsLong(valueIndex);
   }
 
   public String getValueAsString(int valueIndex)
   {
/* 136 */     switch (valueIndex) {
     case 0:
/* 138 */       return this.processId;
     }
     try {
/* 141 */       return Long.toString(getValueAsLong(valueIndex));
     }
     catch (InvalidTypeException e)
     {
     }
 
/* 147 */     throw new ArrayIndexOutOfBoundsException();
   }
 
   static EventValueDescription[] getValueDescriptions()
   {
     try
     {
/* 156 */       return new EventValueDescription[] { new EventValueDescription("Process Name", EventContainer.EventValueType.STRING), new EventValueDescription("GC Time", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.MILLISECONDS), new EventValueDescription("Freed Objects", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.OBJECTS), new EventValueDescription("Freed Bytes", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Soft Limit", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Actual Size (aggregate)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Allowed Size (aggregate)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Allocated Objects (aggregate)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.OBJECTS), new EventValueDescription("Allocated Bytes (aggregate)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Actual Size", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Allowed Size", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Allocated Objects", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.OBJECTS), new EventValueDescription("Allocated Bytes", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Actual Size (zygote)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Allowed Size (zygote)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Allocated Objects (zygote)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.OBJECTS), new EventValueDescription("Allocated Bytes (zygote)", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("External Allocation Limit", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("External Bytes Allocated", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("dlmalloc Footprint", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES), new EventValueDescription("Malloc Info: Total Allocated Space", EventContainer.EventValueType.LONG, EventValueDescription.ValueType.BYTES) };
     }
     catch (InvalidValueTypeException e)
     {
/* 198 */       throw new AssertionError();
 
     }
   }
 
   private void parseDvmHeapInfo(long data, int index) {
/* 206 */     switch (index)
     {
     case 0:
/* 213 */       this.gcTime = float12ToInt((int)(data >> 12 & 0xFFF));
/* 214 */       this.bytesFreed = float12ToInt((int)(data & 0xFFF));
 
/* 218 */       byte[] dataArray = new byte[8];
/* 219 */       put64bitsToArray(data, dataArray, 0);
 
/* 222 */       this.processId = new String(dataArray, 0, 5);
/* 223 */       break;
     case 1:
/* 232 */       this.objectsFreed = float12ToInt((int)(data >> 48 & 0xFFF));
/* 233 */       this.actualSize = float12ToInt((int)(data >> 36 & 0xFFF));
/* 234 */       this.allowedSize = float12ToInt((int)(data >> 24 & 0xFFF));
/* 235 */       this.objectsAllocated = float12ToInt((int)(data >> 12 & 0xFFF));
/* 236 */       this.bytesAllocated = float12ToInt((int)(data & 0xFFF));
/* 237 */       break;
     case 2:
/* 246 */       this.softLimit = float12ToInt((int)(data >> 48 & 0xFFF));
/* 247 */       this.zActualSize = float12ToInt((int)(data >> 36 & 0xFFF));
/* 248 */       this.zAllowedSize = float12ToInt((int)(data >> 24 & 0xFFF));
/* 249 */       this.zObjectsAllocated = float12ToInt((int)(data >> 12 & 0xFFF));
/* 250 */       this.zBytesAllocated = float12ToInt((int)(data & 0xFFF));
/* 251 */       break;
     case 3:
/* 258 */       this.dlmallocFootprint = float12ToInt((int)(data >> 36 & 0xFFF));
/* 259 */       this.mallinfoTotalAllocatedSpace = float12ToInt((int)(data >> 24 & 0xFFF));
/* 260 */       this.externalLimit = float12ToInt((int)(data >> 12 & 0xFFF));
/* 261 */       this.externalBytesAllocated = float12ToInt((int)(data & 0xFFF));
/* 262 */       break;
     }
   }
 
   private static long float12ToInt(int f12)
   {
/* 273 */     return (f12 & 0x1FF) << (f12 >>> 9) * 4;
   }
 
   private static void put64bitsToArray(long value, byte[] dest, int offset)
   {
/* 284 */     dest[(offset + 7)] = ((byte)(int)(value & 0xFF));
/* 285 */     dest[(offset + 6)] = ((byte)(int)((value & 0xFF00) >> 8));
/* 286 */     dest[(offset + 5)] = ((byte)(int)((value & 0xFF0000) >> 16));
/* 287 */     dest[(offset + 4)] = ((byte)(int)((value & 0xFF000000) >> 24));
/* 288 */     dest[(offset + 3)] = ((byte)(int)((value & 0x0) >> 32));
/* 289 */     dest[(offset + 2)] = ((byte)(int)((value & 0x0) >> 40));
/* 290 */     dest[(offset + 1)] = ((byte)(int)((value & 0x0) >> 48));
/* 291 */     dest[(offset + 0)] = ((byte)(int)((value & 0x0) >> 56));
   }
 
   private long getValueAsLong(int valueIndex)
     throws InvalidTypeException
   {
/* 300 */     switch (valueIndex) {
     case 0:
/* 302 */       throw new InvalidTypeException();
     case 1:
/* 304 */       return this.gcTime;
     case 2:
/* 306 */       return this.objectsFreed;
     case 3:
/* 308 */       return this.bytesFreed;
     case 4:
/* 310 */       return this.softLimit;
     case 5:
/* 312 */       return this.actualSize;
     case 6:
/* 314 */       return this.allowedSize;
     case 7:
/* 316 */       return this.objectsAllocated;
     case 8:
/* 318 */       return this.bytesAllocated;
     case 9:
/* 320 */       return this.actualSize - this.zActualSize;
     case 10:
/* 322 */       return this.allowedSize - this.zAllowedSize;
     case 11:
/* 324 */       return this.objectsAllocated - this.zObjectsAllocated;
     case 12:
/* 326 */       return this.bytesAllocated - this.zBytesAllocated;
     case 13:
/* 328 */       return this.zActualSize;
     case 14:
/* 330 */       return this.zAllowedSize;
     case 15:
/* 332 */       return this.zObjectsAllocated;
     case 16:
/* 334 */       return this.zBytesAllocated;
     case 17:
/* 336 */       return this.externalLimit;
     case 18:
/* 338 */       return this.externalBytesAllocated;
     case 19:
/* 340 */       return this.dlmallocFootprint;
     case 20:
/* 342 */       return this.mallinfoTotalAllocatedSpace;
     }
 
/* 345 */     throw new ArrayIndexOutOfBoundsException();
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.GcEventContainer
 * JD-Core Version:    0.6.2
 */