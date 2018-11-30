 package com.android.ddmlib.log;
 
 import java.util.Locale;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class EventContainer
 {
   public int mTag;
   public int pid;
   public int tid;
   public int sec;
   public int nsec;
   private Object mData;
 
   EventContainer(LogReceiver.LogEntry entry, int tag, Object data)
   {
/* 185 */     getType(data);
/* 186 */     this.mTag = tag;
/* 187 */     this.mData = data;
 
/* 189 */     this.pid = entry.pid;
/* 190 */     this.tid = entry.tid;
/* 191 */     this.sec = entry.sec;
/* 192 */     this.nsec = entry.nsec;
   }
 
   EventContainer(int tag, int pid, int tid, int sec, int nsec, Object data)
   {
/* 199 */     getType(data);
/* 200 */     this.mTag = tag;
/* 201 */     this.mData = data;
 
/* 203 */     this.pid = pid;
/* 204 */     this.tid = tid;
/* 205 */     this.sec = sec;
/* 206 */     this.nsec = nsec;
   }
 
   public final Integer getInt()
     throws InvalidTypeException
   {
/* 215 */     if (getType(this.mData) == EventValueType.INT) {
/* 216 */       return (Integer)this.mData;
     }
 
/* 219 */     throw new InvalidTypeException();
   }
 
   public final Long getLong()
     throws InvalidTypeException
   {
/* 228 */     if (getType(this.mData) == EventValueType.LONG) {
/* 229 */       return (Long)this.mData;
     }
 
/* 232 */     throw new InvalidTypeException();
   }
 
   public final String getString()
     throws InvalidTypeException
   {
/* 241 */     if (getType(this.mData) == EventValueType.STRING) {
/* 242 */       return (String)this.mData;
     }
 
/* 245 */     throw new InvalidTypeException();
   }
 
   public Object getValue(int valueIndex)
   {
/* 253 */     return getValue(this.mData, valueIndex, true);
   }
 
   public double getValueAsDouble(int valueIndex)
     throws InvalidTypeException
   {
/* 266 */     return getValueAsDouble(this.mData, valueIndex, true);
   }
 
   public String getValueAsString(int valueIndex)
     throws InvalidTypeException
   {
/* 279 */     return getValueAsString(this.mData, valueIndex, true);
   }
 
   public EventValueType getType()
   {
/* 286 */     return getType(this.mData);
   }
 
   public final EventValueType getType(Object data)
   {
/* 293 */     if ((data instanceof Integer))
/* 294 */       return EventValueType.INT;
/* 295 */     if ((data instanceof Long))
/* 296 */       return EventValueType.LONG;
/* 297 */     if ((data instanceof String))
/* 298 */       return EventValueType.STRING;
/* 299 */     if ((data instanceof Object[]))
     {
/* 301 */       Object[] objects = (Object[])data;
/* 302 */       for (Object obj : objects) {
/* 303 */         EventValueType type = getType(obj);
/* 304 */         if ((type == EventValueType.LIST) || (type == EventValueType.TREE)) {
/* 305 */           return EventValueType.TREE;
         }
       }
/* 308 */       return EventValueType.LIST;
     }
 
/* 311 */     return EventValueType.UNKNOWN;
   }
 
   public boolean testValue(int index, Object value, CompareMethod compareMethod)
     throws InvalidTypeException
   {
/* 326 */     EventValueType type = getType(this.mData);
/* 327 */     if ((index > 0) && (type != EventValueType.LIST)) {
/* 328 */       throw new InvalidTypeException();
     }
 
/* 331 */     Object data = this.mData;
/* 332 */     if (type == EventValueType.LIST) {
/* 333 */       data = ((Object[])(Object[])this.mData)[index];
     }
 
/* 336 */     if (!data.getClass().equals(data.getClass())) {
/* 337 */       throw new InvalidTypeException();
     }
 
/* 340 */     switch (compareMethod.ordinal()) {
     case 1:
/* 342 */       return data.equals(value);
     case 2:
/* 344 */       if ((data instanceof Integer))
/* 345 */         return ((Integer)data).compareTo((Integer)value) <= 0;
/* 346 */       if ((data instanceof Long)) {
/* 347 */         return ((Long)data).compareTo((Long)value) <= 0;
       }
 
/* 351 */       throw new InvalidTypeException();
     case 3:
/* 353 */       if ((data instanceof Integer))
/* 354 */         return ((Integer)data).compareTo((Integer)value) < 0;
/* 355 */       if ((data instanceof Long)) {
/* 356 */         return ((Long)data).compareTo((Long)value) < 0;
       }
 
/* 360 */       throw new InvalidTypeException();
     case 4:
/* 362 */       if ((data instanceof Integer))
/* 363 */         return ((Integer)data).compareTo((Integer)value) >= 0;
/* 364 */       if ((data instanceof Long)) {
/* 365 */         return ((Long)data).compareTo((Long)value) >= 0;
       }
 
/* 369 */       throw new InvalidTypeException();
     case 5:
/* 371 */       if ((data instanceof Integer))
/* 372 */         return ((Integer)data).compareTo((Integer)value) > 0;
/* 373 */       if ((data instanceof Long)) {
/* 374 */         return ((Long)data).compareTo((Long)value) > 0;
       }
 
/* 378 */       throw new InvalidTypeException();
     case 6:
/* 380 */       if ((data instanceof Integer))
/* 381 */         return (((Integer)data).intValue() & ((Integer)value).intValue()) != 0;
/* 382 */       if ((data instanceof Long)) {
/* 383 */         return (((Long)data).longValue() & ((Long)value).longValue()) != 0L;
       }
 
/* 387 */       throw new InvalidTypeException();
     }
/* 389 */     throw new InvalidTypeException();
   }
 
   private Object getValue(Object data, int valueIndex, boolean recursive)
   {
/* 394 */     EventValueType type = getType(data);
 
/* 396 */     switch (type.ordinal()) {
     case 1:
     case 2:
     case 3:
/* 400 */       return data;
     case 4:
/* 402 */       if (recursive) {
/* 403 */         Object[] list = (Object[])data;
/* 404 */         if ((valueIndex >= 0) && (valueIndex < list.length)) {
/* 405 */           return getValue(list[valueIndex], valueIndex, false);
         }
       }
       break;
     }
/* 410 */     return null;
   }
 
   private double getValueAsDouble(Object data, int valueIndex, boolean recursive) throws InvalidTypeException
   {
/* 415 */     EventValueType type = getType(data);
 
/* 417 */     switch (type.ordinal()) {
     case 2:
/* 419 */       return ((Integer)data).doubleValue();
     case 3:
/* 421 */       return ((Long)data).doubleValue();
     case 1:
/* 423 */       throw new InvalidTypeException();
     case 4:
/* 425 */       if (recursive) {
/* 426 */         Object[] list = (Object[])data;
/* 427 */         if ((valueIndex >= 0) && (valueIndex < list.length)) {
/* 428 */           return getValueAsDouble(list[valueIndex], valueIndex, false);
         }
       }
       break;
     }
/* 433 */     throw new InvalidTypeException();
   }
 
   private String getValueAsString(Object data, int valueIndex, boolean recursive) throws InvalidTypeException
   {
/* 438 */     EventValueType type = getType(data);
 
/* 440 */     switch (type.ordinal()) {
     case 2:
/* 442 */       return data.toString();
     case 3:
/* 444 */       return data.toString();
     case 1:
/* 446 */       return (String)data;
     case 4:
/* 448 */       if (recursive) {
/* 449 */         Object[] list = (Object[])data;
/* 450 */         if ((valueIndex >= 0) && (valueIndex < list.length))
/* 451 */           return getValueAsString(list[valueIndex], valueIndex, false);
       }
       else {
/* 454 */         throw new InvalidTypeException("getValueAsString() doesn't support EventValueType.TREE");
       }
       break;
     }
 
/* 459 */     throw new InvalidTypeException("getValueAsString() unsupported type:" + type);
   }
 
   public static enum EventValueType
   {
/*  71 */     UNKNOWN(0), 
/*  72 */     INT(1), 
/*  73 */     LONG(2), 
/*  74 */     STRING(3), 
/*  75 */     LIST(4), 
/*  76 */     TREE(5);
 
/*  78 */     private static final Pattern STORAGE_PATTERN = Pattern.compile("^(\\d+)@(.*)$");
     private int mValue;
 
     static EventValueType getEventValueType(int value)
     {
/*  88 */       for (EventValueType type : values()) {
/*  89 */         if (type.mValue == value) {
/*  90 */           return type;
         }
       }
 
/*  94 */       return null;
     }
 
     public static String getStorageString(Object object)
     {
/* 109 */       if ((object instanceof String))
/* 110 */         return STRING.mValue + "@" + object;
/* 111 */       if ((object instanceof Integer))
/* 112 */         return INT.mValue + "@" + object.toString();
/* 113 */       if ((object instanceof Long)) {
/* 114 */         return LONG.mValue + "@" + object.toString();
       }
 
/* 117 */       return null;
     }
 
     public static Object getObjectFromStorageString(String value)
     {
/* 127 */       Matcher m = STORAGE_PATTERN.matcher(value);
/* 128 */       if (m.matches()) {
         try {
/* 130 */           EventValueType type = getEventValueType(Integer.parseInt(m.group(1)));
 
/* 132 */           if (type == null) {
/* 133 */             return null;
           }
 
/* 136 */           switch (type.ordinal()) {
           case 1:
/* 138 */             return m.group(2);
           case 2:
/* 140 */             return Integer.valueOf(m.group(2));
           case 3:
/* 142 */             return Long.valueOf(m.group(2));
           }
         } catch (NumberFormatException nfe) {
/* 145 */           return null;
         }
       }
 
/* 149 */       return null;
     }
 
     public int getValue()
     {
/* 157 */       return this.mValue;
     }
 
     public String toString()
     {
/* 162 */       return super.toString().toLowerCase(Locale.US);
     }
 
     private EventValueType(int value) {
/* 166 */       this.mValue = value;
     }
   }
 
   public static enum CompareMethod
   {
/*  35 */     EQUAL_TO("equals", "=="), 
/*  36 */     LESSER_THAN("less than or equals to", "<="), 
/*  37 */     LESSER_THAN_STRICT("less than", "<"), 
/*  38 */     GREATER_THAN("greater than or equals to", ">="), 
/*  39 */     GREATER_THAN_STRICT("greater than", ">"), 
/*  40 */     BIT_CHECK("bit check", "&");
 
     private final String mName;
     private final String mTestString;
 
/*  46 */     private CompareMethod(String name, String testString) { this.mName = name;
/*  47 */       this.mTestString = testString;
     }
 
     public String toString()
     {
/*  55 */       return this.mName;
     }
 
     public String testString()
     {
/*  62 */       return this.mTestString;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.EventContainer
 * JD-Core Version:    0.6.2
 */