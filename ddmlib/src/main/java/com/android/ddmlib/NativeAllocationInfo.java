 package com.android.ddmlib;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Iterator;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class NativeAllocationInfo
 {
   public static final String END_STACKTRACE_KW = "EndStacktrace";
   public static final String BEGIN_STACKTRACE_KW = "BeginStacktrace:";
   public static final String TOTAL_SIZE_KW = "TotalSize:";
   public static final String SIZE_KW = "Size:";
   public static final String ALLOCATIONS_KW = "Allocations:";
   private static final int FLAG_ZYGOTE_CHILD = -2147483648;
   private static final int FLAG_MASK = -2147483648;
/*  44 */   private static final List<String> FILTERED_LIBRARIES = Arrays.asList(new String[] { "libc.so", "libc_malloc_debug_leak.so" });
 
/*  50 */   private static final List<Pattern> FILTERED_METHOD_NAME_PATTERNS = Arrays.asList(new Pattern[] { Pattern.compile("malloc", 2), Pattern.compile("calloc", 2), Pattern.compile("realloc", 2), Pattern.compile("operator new", 2), Pattern.compile("memalign", 2) });
   private final int mSize;
   private final boolean mIsZygoteChild;
   private int mAllocations;
/*  63 */   private final ArrayList<Long> mStackCallAddresses = new ArrayList();
 
/*  65 */   private ArrayList<NativeStackCallInfo> mResolvedStackCall = null;
 
/*  67 */   private boolean mIsStackCallResolved = false;
 
   public NativeAllocationInfo(int size, int allocations)
   {
/*  75 */     this.mSize = (size & 0x7FFFFFFF);
/*  76 */     this.mIsZygoteChild = ((size & 0x80000000) != 0);
/*  77 */     this.mAllocations = allocations;
   }
 
   public void addStackCallAddress(long address)
   {
/*  85 */     this.mStackCallAddresses.add(Long.valueOf(address));
   }
 
   public int getSize()
   {
/*  92 */     return this.mSize;
   }
 
   public boolean isZygoteChild()
   {
/* 100 */     return this.mIsZygoteChild;
   }
 
   public int getAllocationCount()
   {
/* 107 */     return this.mAllocations;
   }
 
   public boolean isStackCallResolved()
   {
/* 115 */     return this.mIsStackCallResolved;
   }
 
   public List<Long> getStackCallAddresses()
   {
/* 123 */     return this.mStackCallAddresses;
   }
 
   public synchronized void setResolvedStackCall(List<NativeStackCallInfo> resolvedStackCall)
   {
/* 134 */     if (this.mResolvedStackCall == null)
/* 135 */       this.mResolvedStackCall = new ArrayList();
     else {
/* 137 */       this.mResolvedStackCall.clear();
     }
/* 139 */     this.mResolvedStackCall.addAll(resolvedStackCall);
/* 140 */     this.mIsStackCallResolved = (!this.mResolvedStackCall.isEmpty());
   }
 
   public synchronized List<NativeStackCallInfo> getResolvedStackCall()
   {
/* 151 */     if (this.mIsStackCallResolved) {
/* 152 */       return this.mResolvedStackCall;
     }
 
/* 155 */     return null;
   }
 
   public boolean equals(Object obj)
   {
/* 167 */     if (obj == this)
/* 168 */       return true;
/* 169 */     if ((obj instanceof NativeAllocationInfo)) {
/* 170 */       NativeAllocationInfo mi = (NativeAllocationInfo)obj;
 
/* 172 */       if ((this.mSize != mi.mSize) || (this.mAllocations != mi.mAllocations)) {
/* 173 */         return false;
       }
 
/* 177 */       return stackEquals(mi);
     }
/* 179 */     return false;
   }
 
   public boolean stackEquals(NativeAllocationInfo mi) {
/* 183 */     if (this.mStackCallAddresses.size() != mi.mStackCallAddresses.size()) {
/* 184 */       return false;
     }
 
/* 187 */     int count = this.mStackCallAddresses.size();
/* 188 */     for (int i = 0; i < count; i++) {
/* 189 */       long a = ((Long)this.mStackCallAddresses.get(i)).longValue();
/* 190 */       long b = ((Long)mi.mStackCallAddresses.get(i)).longValue();
/* 191 */       if (a != b) {
/* 192 */         return false;
       }
     }
 
/* 196 */     return true;
   }
 
   public int hashCode()
   {
/* 205 */     int result = 17;
 
/* 207 */     result = 31 * result + this.mSize;
/* 208 */     result = 31 * result + this.mAllocations;
/* 209 */     result = 31 * result + this.mStackCallAddresses.size();
 
/* 211 */     for (Iterator i$ = this.mStackCallAddresses.iterator(); i$.hasNext(); ) { long addr = ((Long)i$.next()).longValue();
/* 212 */       result = 31 * result + (int)(addr ^ addr >>> 32);
     }
 
/* 215 */     return result;
   }
 
   public String toString()
   {
/* 224 */     StringBuilder buffer = new StringBuilder();
/* 225 */     buffer.append("Allocations:");
/* 226 */     buffer.append(' ');
/* 227 */     buffer.append(this.mAllocations);
/* 228 */     buffer.append('\n');
 
/* 230 */     buffer.append("Size:");
/* 231 */     buffer.append(' ');
/* 232 */     buffer.append(this.mSize);
/* 233 */     buffer.append('\n');
 
/* 235 */     buffer.append("TotalSize:");
/* 236 */     buffer.append(' ');
/* 237 */     buffer.append(this.mSize * this.mAllocations);
/* 238 */     buffer.append('\n');
 
/* 240 */     if (this.mResolvedStackCall != null) {
/* 241 */       buffer.append("BeginStacktrace:");
/* 242 */       buffer.append('\n');
/* 243 */       for (NativeStackCallInfo source : this.mResolvedStackCall) {
/* 244 */         long addr = source.getAddress();
/* 245 */         if (addr != 0L)
         {
/* 249 */           if (source.getLineNumber() != -1) {
/* 250 */             buffer.append(String.format("\t%1$08x\t%2$s --- %3$s --- %4$s:%5$d\n", new Object[] { Long.valueOf(addr), source.getLibraryName(), source.getMethodName(), source.getSourceFile(), Integer.valueOf(source.getLineNumber()) }));
           }
           else
           {
/* 254 */             buffer.append(String.format("\t%1$08x\t%2$s --- %3$s --- %4$s\n", new Object[] { Long.valueOf(addr), source.getLibraryName(), source.getMethodName(), source.getSourceFile() }));
           }
         }
       }
/* 258 */       buffer.append("EndStacktrace");
/* 259 */       buffer.append('\n');
     }
 
/* 262 */     return buffer.toString();
   }
 
   public synchronized NativeStackCallInfo getRelevantStackCallInfo()
   {
/* 276 */     if ((this.mIsStackCallResolved) && (this.mResolvedStackCall != null)) {
/* 277 */       for (NativeStackCallInfo info : this.mResolvedStackCall) {
/* 278 */         if ((isRelevantLibrary(info.getLibraryName())) && (isRelevantMethod(info.getMethodName())))
         {
/* 280 */           return info;
         }
 
       }
 
/* 285 */       if (!this.mResolvedStackCall.isEmpty()) {
/* 286 */         return (NativeStackCallInfo)this.mResolvedStackCall.get(0);
       }
     }
/* 289 */     return null;
   }
 
   private boolean isRelevantLibrary(String libPath) {
/* 293 */     for (String l : FILTERED_LIBRARIES) {
/* 294 */       if (libPath.endsWith(l)) {
/* 295 */         return false;
       }
     }
 
/* 299 */     return true;
   }
 
   private boolean isRelevantMethod(String methodName) {
/* 303 */     for (Pattern p : FILTERED_METHOD_NAME_PATTERNS) {
/* 304 */       Matcher m = p.matcher(methodName);
/* 305 */       if (m.find()) {
/* 306 */         return false;
       }
     }
 
/* 310 */     return true;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.NativeAllocationInfo
 * JD-Core Version:    0.6.2
 */