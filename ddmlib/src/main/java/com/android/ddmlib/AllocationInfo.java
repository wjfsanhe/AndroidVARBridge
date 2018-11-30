 package com.android.ddmlib;
 
 import com.google.common.collect.Lists;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 import java.util.Locale;
 
 public class AllocationInfo
   implements IStackTraceInfo
 {
   private final String mAllocatedClass;
   private final int mAllocNumber;
   private final int mAllocationSize;
   private final short mThreadId;
   private final StackTraceElement[] mStackTrace;
 
   AllocationInfo(int allocNumber, String allocatedClass, int allocationSize, short threadId, StackTraceElement[] stackTrace)
   {
/* 141 */     this.mAllocNumber = allocNumber;
/* 142 */     this.mAllocatedClass = allocatedClass;
/* 143 */     this.mAllocationSize = allocationSize;
/* 144 */     this.mThreadId = threadId;
/* 145 */     this.mStackTrace = stackTrace;
   }
 
   public int getAllocNumber()
   {
/* 153 */     return this.mAllocNumber;
   }
 
   public String getAllocatedClass()
   {
/* 160 */     return this.mAllocatedClass;
   }
 
   public int getSize()
   {
/* 167 */     return this.mAllocationSize;
   }
 
   public short getThreadId()
   {
/* 174 */     return this.mThreadId;
   }
 
   public StackTraceElement[] getStackTrace()
   {
/* 183 */     return this.mStackTrace;
   }
 
   public int compareTo(AllocationInfo otherAlloc) {
/* 187 */     return otherAlloc.mAllocationSize - this.mAllocationSize;
   }
 
   public String getAllocationSite()
   {
/* 192 */     if (this.mStackTrace.length > 0) {
/* 193 */       return this.mStackTrace[0].toString();
     }
/* 195 */     return null;
   }
 
   public String getFirstTraceClassName() {
/* 199 */     if (this.mStackTrace.length > 0) {
/* 200 */       return this.mStackTrace[0].getClassName();
     }
 
/* 203 */     return null;
   }
 
   public String getFirstTraceMethodName() {
/* 207 */     if (this.mStackTrace.length > 0) {
/* 208 */       return this.mStackTrace[0].getMethodName();
     }
 
/* 211 */     return null;
   }
 
   public boolean filter(String filter, boolean fullTrace, Locale locale)
   {
/* 219 */     return (allocatedClassMatches(filter, locale)) || (!getMatchingStackFrames(filter, fullTrace, locale).isEmpty());
   }
 
   public boolean allocatedClassMatches(String pattern, Locale locale) {
/* 223 */     return this.mAllocatedClass.toLowerCase(locale).contains(pattern.toLowerCase(locale));
   }
 
   public List<String> getMatchingStackFrames(String filter, boolean fullTrace, Locale locale)
   {
/* 228 */     filter = filter.toLowerCase(locale);
 
/* 230 */     if (this.mStackTrace.length > 0) {
/* 231 */       int length = fullTrace ? this.mStackTrace.length : 1;
/* 232 */       List matchingFrames = Lists.newArrayListWithExpectedSize(length);
/* 233 */       for (int i = 0; i < length; i++) {
/* 234 */         String frameString = this.mStackTrace[i].toString();
/* 235 */         if (frameString.toLowerCase(locale).contains(filter)) {
/* 236 */           matchingFrames.add(frameString);
         }
       }
/* 239 */       return matchingFrames;
     }
/* 241 */     return Collections.emptyList();
   }
 
   public static final class AllocationSorter
     implements Comparator<AllocationInfo>
   {
/*  44 */     private AllocationInfo.SortMode mSortMode = AllocationInfo.SortMode.SIZE;
/*  45 */     private boolean mDescending = true;
 
     public void setSortMode(AllocationInfo.SortMode mode)
     {
/*  51 */       if (this.mSortMode == mode)
/*  52 */         this.mDescending = (!this.mDescending);
       else
/*  54 */         this.mSortMode = mode;
     }
 
     public void setSortMode(AllocationInfo.SortMode mode, boolean descending)
     {
/*  59 */       this.mSortMode = mode;
/*  60 */       this.mDescending = descending;
     }
 
     public AllocationInfo.SortMode getSortMode()
     {
/*  65 */       return this.mSortMode;
     }
 
     public boolean isDescending() {
/*  69 */       return this.mDescending;
     }
 
     public int compare(AllocationInfo o1, AllocationInfo o2)
     {
/*  74 */       int diff = 0;
/*  75 */       switch (this.mSortMode.ordinal()) {
       case 1:
/*  77 */         diff = o1.mAllocNumber - o2.mAllocNumber;
/*  78 */         break;
       case 2:
/*  82 */         break;
       case 3:
/*  84 */         diff = o1.mAllocatedClass.compareTo(o2.mAllocatedClass);
/*  85 */         break;
       case 4:
/*  87 */         diff = o1.mThreadId - o2.mThreadId;
/*  88 */         break;
       case 5:
/*  90 */         String class1 = o1.getFirstTraceClassName();
/*  91 */         String class2 = o2.getFirstTraceClassName();
/*  92 */         diff = compareOptionalString(class1, class2);
/*  93 */         break;
       case 6:
/*  95 */         String method1 = o1.getFirstTraceMethodName();
/*  96 */         String method2 = o2.getFirstTraceMethodName();
/*  97 */         diff = compareOptionalString(method1, method2);
/*  98 */         break;
       case 7:
/* 100 */         String desc1 = o1.getAllocationSite();
/* 101 */         String desc2 = o2.getAllocationSite();
/* 102 */         diff = compareOptionalString(desc1, desc2);
       }
 
/* 106 */       if (diff == 0)
       {
/* 108 */         diff = o1.mAllocationSize - o2.mAllocationSize;
       }
 
/* 111 */       if (this.mDescending) {
/* 112 */         diff = -diff;
       }
 
/* 115 */       return diff;
     }
 
     private static int compareOptionalString(String str1, String str2)
     {
/* 120 */       if (str1 != null) {
/* 121 */         if (str2 == null) {
/* 122 */           return -1;
         }
/* 124 */         return str1.compareTo(str2);
       }
 
/* 127 */       if (str2 == null) {
/* 128 */         return 0;
       }
/* 130 */       return 1;
     }
   }
 
   public static enum SortMode
   {
/*  39 */     NUMBER, SIZE, CLASS, THREAD, ALLOCATION_SITE, IN_CLASS, IN_METHOD;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.AllocationInfo
 * JD-Core Version:    0.6.2
 */