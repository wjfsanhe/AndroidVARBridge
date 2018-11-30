 package com.android.ddmlib;
 
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public final class NativeStackCallInfo
 {
/*  27 */   private static final Pattern SOURCE_NAME_PATTERN = Pattern.compile("^(.+):(\\d+)(\\s+\\(discriminator\\s+\\d+\\))?$");
   private long mAddress;
   private String mLibrary;
   private String mMethod;
   private String mSourceFile;
/*  44 */   private int mLineNumber = -1;
 
   public NativeStackCallInfo(long address, String lib, String method, String sourceFile)
   {
/*  56 */     this.mAddress = address;
/*  57 */     this.mLibrary = lib;
/*  58 */     this.mMethod = method;
 
/*  60 */     Matcher m = SOURCE_NAME_PATTERN.matcher(sourceFile);
/*  61 */     if (m.matches()) {
/*  62 */       this.mSourceFile = m.group(1);
       try {
/*  64 */         this.mLineNumber = Integer.parseInt(m.group(2));
       }
       catch (NumberFormatException e) {
       }
/*  68 */       if ((m.groupCount() == 3) && (m.group(3) != null))
       {
/*  70 */         this.mSourceFile += m.group(3);
       }
     } else {
/*  73 */       this.mSourceFile = sourceFile;
     }
   }
 
   public long getAddress()
   {
/*  81 */     return this.mAddress;
   }
 
   public String getLibraryName()
   {
/*  88 */     return this.mLibrary;
   }
 
   public String getMethodName()
   {
/*  95 */     return this.mMethod;
   }
 
   public String getSourceFile()
   {
/* 102 */     return this.mSourceFile;
   }
 
   public int getLineNumber()
   {
/* 109 */     return this.mLineNumber;
   }
 
   public String toString()
   {
/* 114 */     return String.format("\t%1$08x\t%2$s --- %3$s --- %4$s:%5$d", new Object[] { Long.valueOf(getAddress()), getLibraryName(), getMethodName(), getSourceFile(), Integer.valueOf(getLineNumber()) });
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.NativeStackCallInfo
 * JD-Core Version:    0.6.2
 */