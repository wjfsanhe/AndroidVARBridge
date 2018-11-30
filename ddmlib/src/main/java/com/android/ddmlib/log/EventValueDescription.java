 package com.android.ddmlib.log;
 
 import java.util.Locale;
 
 public final class EventValueDescription
 {
   private String mName;
   private EventContainer.EventValueType mEventValueType;
   private ValueType mValueType;
 
   EventValueDescription(String name, EventContainer.EventValueType type)
   {
/* 110 */     this.mName = name;
/* 111 */     this.mEventValueType = type;
/* 112 */     if ((this.mEventValueType == EventContainer.EventValueType.INT) || (this.mEventValueType == EventContainer.EventValueType.LONG))
/* 113 */       this.mValueType = ValueType.BYTES;
     else
/* 115 */       this.mValueType = ValueType.NOT_APPLICABLE;
   }
 
   EventValueDescription(String name, EventContainer.EventValueType type, ValueType valueType)
     throws InvalidValueTypeException
   {
/* 130 */     this.mName = name;
/* 131 */     this.mEventValueType = type;
/* 132 */     this.mValueType = valueType;
/* 133 */     this.mValueType.checkType(this.mEventValueType);
   }
 
   public String getName()
   {
/* 140 */     return this.mName;
   }
 
   public EventContainer.EventValueType getEventValueType()
   {
/* 147 */     return this.mEventValueType;
   }
 
   public ValueType getValueType()
   {
/* 154 */     return this.mValueType;
   }
 
   public String toString()
   {
/* 159 */     if (this.mValueType != ValueType.NOT_APPLICABLE) {
/* 160 */       return String.format("%1$s (%2$s, %3$s)", new Object[] { this.mName, this.mEventValueType.toString(), this.mValueType.toString() });
     }
 
/* 164 */     return String.format("%1$s (%2$s)", new Object[] { this.mName, this.mEventValueType.toString() });
   }
 
   public boolean checkForType(Object value)
   {
/* 173 */     switch (this.mEventValueType.ordinal()) {
     case 1:
/* 175 */       return value instanceof Integer;
     case 2:
/* 177 */       return value instanceof Long;
     case 3:
/* 179 */       return value instanceof String;
     case 4:
/* 181 */       return value instanceof Object[];
     }
 
/* 184 */     return false;
   }
 
   public Object getObjectFromString(String value)
   {
/* 197 */     switch (this.mEventValueType.ordinal()) {
     case 1:
       try {
/* 200 */         return Integer.valueOf(value);
       } catch (NumberFormatException e) {
/* 202 */         return null;
       }
     case 2:
       try {
/* 206 */         return Long.valueOf(value);
       } catch (NumberFormatException e) {
/* 208 */         return null;
       }
     case 3:
/* 211 */       return value;
     }
 
/* 214 */     return null;
   }
 
   public static enum ValueType
   {
/*  42 */     NOT_APPLICABLE(0), 
/*  43 */     OBJECTS(1), 
/*  44 */     BYTES(2), 
/*  45 */     MILLISECONDS(3), 
/*  46 */     ALLOCATIONS(4), 
/*  47 */     ID(5), 
/*  48 */     PERCENT(6);
 
     private int mValue;
 
     public void checkType(EventContainer.EventValueType type)
       throws InvalidValueTypeException
     {
/*  58 */       if ((type != EventContainer.EventValueType.INT) && (type != EventContainer.EventValueType.LONG) && (this != NOT_APPLICABLE))
       {
/*  60 */         throw new InvalidValueTypeException(String.format("%1$s doesn't support type %2$s", new Object[] { type, this }));
       }
     }
 
     public static ValueType getValueType(int value)
     {
/*  71 */       for (ValueType type : values()) {
/*  72 */         if (type.mValue == value) {
/*  73 */           return type;
         }
       }
/*  76 */       return null;
     }
 
     public int getValue()
     {
/*  83 */       return this.mValue;
     }
 
     public String toString()
     {
/*  88 */       return super.toString().toLowerCase(Locale.US);
     }
 
     private ValueType(int value) {
/*  92 */       this.mValue = value;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.EventValueDescription
 * JD-Core Version:    0.6.2
 */