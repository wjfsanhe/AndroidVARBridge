 package com.android.ddmlib.logcat;
 
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public final class LogCatTimestamp
 {
/*  29 */   public static final LogCatTimestamp ZERO = new LogCatTimestamp(1, 1, 0, 0, 0, 0);
   private final int mMonth;
   private final int mDay;
   private final int mHour;
   private final int mMinute;
   private final int mSecond;
   private final int mMilli;
/*  38 */   private static final Pattern sTimePattern = Pattern.compile("^(\\d\\d)-(\\d\\d)\\s(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)$");
 
   public static LogCatTimestamp fromString(String timeString)
   {
/*  42 */     Matcher matcher = sTimePattern.matcher(timeString);
/*  43 */     if (!matcher.matches()) {
/*  44 */       throw new IllegalArgumentException("Invalid timestamp. Expected MM-DD HH:MM:SS:mmm");
     }
 
/*  47 */     int month = Integer.parseInt(matcher.group(1));
/*  48 */     int day = Integer.parseInt(matcher.group(2));
/*  49 */     int hour = Integer.parseInt(matcher.group(3));
/*  50 */     int minute = Integer.parseInt(matcher.group(4));
/*  51 */     int second = Integer.parseInt(matcher.group(5));
/*  52 */     int millisecond = Integer.parseInt(matcher.group(6));
 
/*  55 */     while (millisecond >= 1000) {
/*  56 */       millisecond /= 10;
     }
 
/*  59 */     return new LogCatTimestamp(month, day, hour, minute, second, millisecond);
   }
 
   public LogCatTimestamp(int month, int day, int hour, int minute, int second, int milli)
   {
/*  66 */     if ((month < 1) || (month > 12)) {
/*  67 */       throw new IllegalArgumentException(String.format("Month should be between 1-12: %d", new Object[] { Integer.valueOf(month) }));
     }
 
/*  71 */     if ((day < 1) || (day > 31)) {
/*  72 */       throw new IllegalArgumentException(String.format("Day should be between 1-31: %d", new Object[] { Integer.valueOf(day) }));
     }
 
/*  76 */     if ((hour < 0) || (hour > 23)) {
/*  77 */       throw new IllegalArgumentException(String.format("Hour should be between 0-23: %d", new Object[] { Integer.valueOf(hour) }));
     }
 
/*  81 */     if ((minute < 0) || (minute > 59)) {
/*  82 */       throw new IllegalArgumentException(String.format("Minute should be between 0-59: %d", new Object[] { Integer.valueOf(minute) }));
     }
 
/*  86 */     if ((second < 0) || (second > 59)) {
/*  87 */       throw new IllegalArgumentException(String.format("Second should be between 0-59 %d", new Object[] { Integer.valueOf(second) }));
     }
 
/*  91 */     if ((milli < 0) || (milli > 999)) {
/*  92 */       throw new IllegalArgumentException(String.format("Millisecond should be between 0-999: %d", new Object[] { Integer.valueOf(milli) }));
     }
 
/*  96 */     this.mMonth = month;
/*  97 */     this.mDay = day;
/*  98 */     this.mHour = hour;
/*  99 */     this.mMinute = minute;
/* 100 */     this.mSecond = second;
/* 101 */     this.mMilli = milli;
   }
 
   public boolean equals(Object o)
   {
/* 106 */     if (this == o) return true;
/* 107 */     if ((o == null) || (getClass() != o.getClass())) return false;
 
/* 109 */     LogCatTimestamp that = (LogCatTimestamp)o;
 
/* 111 */     if (this.mMonth != that.mMonth) return false;
/* 112 */     if (this.mDay != that.mDay) return false;
/* 113 */     if (this.mHour != that.mHour) return false;
/* 114 */     if (this.mMinute != that.mMinute) return false;
/* 115 */     if (this.mSecond != that.mSecond) return false;
/* 116 */     if (this.mMilli != that.mMilli) return false;
 
/* 118 */     return true;
   }
 
   public boolean isBefore(LogCatTimestamp other) {
/* 122 */     if ((this.mMonth == 12) && (other.mMonth == 1))
     {
/* 131 */       return true;
     }
/* 133 */     if ((this.mMonth == 1) && (other.mMonth == 12)) {
/* 134 */       return false;
     }
 
/* 137 */     if (this.mMonth < other.mMonth) {
/* 138 */       return true;
     }
/* 140 */     if (this.mMonth > other.mMonth) {
/* 141 */       return false;
     }
 
/* 144 */     if (this.mDay < other.mDay) {
/* 145 */       return true;
     }
/* 147 */     if (this.mDay > other.mDay) {
/* 148 */       return false;
     }
 
/* 151 */     if (this.mHour < other.mHour) {
/* 152 */       return true;
     }
/* 154 */     if (this.mHour > other.mHour) {
/* 155 */       return false;
     }
 
/* 158 */     if (this.mMinute < other.mMinute) {
/* 159 */       return true;
     }
/* 161 */     if (this.mMinute > other.mMinute) {
/* 162 */       return false;
     }
 
/* 165 */     if (this.mSecond < other.mSecond) {
/* 166 */       return true;
     }
/* 168 */     if (this.mSecond > other.mSecond) {
/* 169 */       return false;
     }
 
/* 172 */     if (this.mMilli < other.mMilli) {
/* 173 */       return true;
     }
/* 175 */     if (this.mMilli > other.mMilli) {
/* 176 */       return false;
     }
 
/* 179 */     return false;
   }
 
   public String toString()
   {
/* 185 */     return String.format("%02d-%02d %02d:%02d:%02d.%03d", new Object[] { Integer.valueOf(this.mMonth), Integer.valueOf(this.mDay), Integer.valueOf(this.mHour), Integer.valueOf(this.mMinute), Integer.valueOf(this.mSecond), Integer.valueOf(this.mMilli) });
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatTimestamp
 * JD-Core Version:    0.6.2
 */