 package com.android.ddmlib;
 
 import java.util.Locale;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class AdbVersion
   implements Comparable<AdbVersion>
 {
/* 25 */   public static final AdbVersion UNKNOWN = new AdbVersion(-1, -1, -1);
 
/* 28 */   private static final Pattern ADB_VERSION_PATTERN = Pattern.compile("^.*(\\d+)\\.(\\d+)\\.(\\d+).*");
   public final int major;
   public final int minor;
   public final int micro;
 
   private AdbVersion(int major, int minor, int micro)
   {
/* 36 */     this.major = major;
/* 37 */     this.minor = minor;
/* 38 */     this.micro = micro;
   }
 
   public String toString()
   {
/* 43 */     return String.format(Locale.US, "%1$d.%2$d.%3$d", new Object[] { Integer.valueOf(this.major), Integer.valueOf(this.minor), Integer.valueOf(this.micro) });
   }
 
   public int compareTo(AdbVersion o)
   {
/* 48 */     if (this.major != o.major) {
/* 49 */       return this.major - o.major;
     }
 
/* 52 */     if (this.minor != o.minor) {
/* 53 */       return this.minor - o.minor;
     }
 
/* 56 */     return this.micro - o.micro;
   }
 
   public static AdbVersion parseFrom(String input)
   {
/* 61 */     Matcher matcher = ADB_VERSION_PATTERN.matcher(input);
/* 62 */     if (matcher.matches()) {
/* 63 */       int major = Integer.parseInt(matcher.group(1));
/* 64 */       int minor = Integer.parseInt(matcher.group(2));
/* 65 */       int micro = Integer.parseInt(matcher.group(3));
/* 66 */       return new AdbVersion(major, minor, micro);
     }
/* 68 */     return UNKNOWN;
   }
 
   public boolean equals(Object o)
   {
/* 74 */     if (this == o) {
/* 75 */       return true;
     }
/* 77 */     if ((o == null) || (getClass() != o.getClass())) {
/* 78 */       return false;
     }
 
/* 81 */     AdbVersion version = (AdbVersion)o;
 
/* 83 */     if (this.major != version.major) {
/* 84 */       return false;
     }
/* 86 */     if (this.minor != version.minor) {
/* 87 */       return false;
     }
/* 89 */     return this.micro == version.micro;
   }
 
   public int hashCode()
   {
/* 95 */     int result = this.major;
/* 96 */     result = 31 * result + this.minor;
/* 97 */     result = 31 * result + this.micro;
/* 98 */     return result;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.AdbVersion
 * JD-Core Version:    0.6.2
 */