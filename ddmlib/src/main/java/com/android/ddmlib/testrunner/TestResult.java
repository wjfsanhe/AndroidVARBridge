 package com.android.ddmlib.testrunner;
 
 import java.util.Arrays;
 import java.util.Map;
 
 public class TestResult
 {
   private TestStatus mStatus;
   private String mStackTrace;
   private Map<String, String> mMetrics;
/*  45 */   private long mStartTime = 0L;
/*  46 */   private long mEndTime = 0L;
 
   public TestResult() {
/*  49 */     this.mStatus = TestStatus.INCOMPLETE;
/*  50 */     this.mStartTime = System.currentTimeMillis();
   }
 
   public TestStatus getStatus()
   {
/*  57 */     return this.mStatus;
   }
 
   public String getStackTrace()
   {
/*  65 */     return this.mStackTrace;
   }
 
   public Map<String, String> getMetrics()
   {
/*  72 */     return this.mMetrics;
   }
 
   public void setMetrics(Map<String, String> metrics)
   {
/*  79 */     this.mMetrics = metrics;
   }
 
   public long getStartTime()
   {
/*  87 */     return this.mStartTime;
   }
 
   public long getEndTime()
   {
/*  95 */     return this.mEndTime;
   }
 
   public TestResult setStatus(TestStatus status)
   {
/* 102 */     this.mStatus = status;
/* 103 */     return this;
   }
 
   public void setStackTrace(String trace)
   {
/* 110 */     this.mStackTrace = trace;
   }
 
   public void setEndTime(long currentTimeMillis)
   {
/* 117 */     this.mEndTime = currentTimeMillis;
   }
 
   public int hashCode()
   {
/* 122 */     return Arrays.hashCode(new Object[] { this.mMetrics, this.mStackTrace, this.mStatus });
   }
 
   public boolean equals(Object obj)
   {
/* 127 */     if (this == obj) {
/* 128 */       return true;
     }
/* 130 */     if (obj == null) {
/* 131 */       return false;
     }
/* 133 */     if (getClass() != obj.getClass()) {
/* 134 */       return false;
     }
/* 136 */     TestResult other = (TestResult)obj;
/* 137 */     return (equal(this.mMetrics, other.mMetrics)) && (equal(this.mStackTrace, other.mStackTrace)) && (equal(this.mStatus, other.mStatus));
   }
 
   private static boolean equal(Object a, Object b)
   {
/* 143 */     return (a == b) || ((a != null) && (a.equals(b)));
   }
 
   public static enum TestStatus
   {
/*  30 */     FAILURE, 
 
/*  32 */     PASSED, 
 
/*  34 */     INCOMPLETE, 
 
/*  36 */     ASSUMPTION_FAILURE, 
 
/*  38 */     IGNORED;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.TestResult
 * JD-Core Version:    0.6.2
 */