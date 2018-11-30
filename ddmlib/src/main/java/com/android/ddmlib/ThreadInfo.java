 package com.android.ddmlib;
 
 public final class ThreadInfo
   implements IStackTraceInfo
 {
   private int mThreadId;
   private String mThreadName;
   private int mStatus;
   private int mTid;
   private int mUtime;
   private int mStime;
   private boolean mIsDaemon;
   private StackTraceElement[] mTrace;
   private long mTraceTime;
 
   ThreadInfo(int threadId, String threadName)
   {
/*  41 */     this.mThreadId = threadId;
/*  42 */     this.mThreadName = threadName;
 
/*  44 */     this.mStatus = -1;
   }
 
   void updateThread(int status, int tid, int utime, int stime, boolean isDaemon)
   {
/*  54 */     this.mStatus = status;
/*  55 */     this.mTid = tid;
/*  56 */     this.mUtime = utime;
/*  57 */     this.mStime = stime;
/*  58 */     this.mIsDaemon = isDaemon;
   }
 
   void setStackCall(StackTraceElement[] trace)
   {
/*  66 */     this.mTrace = trace;
/*  67 */     this.mTraceTime = System.currentTimeMillis();
   }
 
   public int getThreadId()
   {
/*  74 */     return this.mThreadId;
   }
 
   public String getThreadName()
   {
/*  81 */     return this.mThreadName;
   }
 
   void setThreadName(String name) {
/*  85 */     this.mThreadName = name;
   }
 
   public int getTid()
   {
/*  92 */     return this.mTid;
   }
 
   public int getStatus()
   {
/*  99 */     return this.mStatus;
   }
 
   public int getUtime()
   {
/* 106 */     return this.mUtime;
   }
 
   public int getStime()
   {
/* 113 */     return this.mStime;
   }
 
   public boolean isDaemon()
   {
/* 120 */     return this.mIsDaemon;
   }
 
   public StackTraceElement[] getStackTrace()
   {
/* 129 */     return this.mTrace;
   }
 
   public long getStackCallTime()
   {
/* 137 */     return this.mTraceTime;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.ThreadInfo
 * JD-Core Version:    0.6.2
 */