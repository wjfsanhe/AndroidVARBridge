 package com.android.ddmlib.testrunner;
 
 import com.android.ddmlib.Log;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.LinkedHashSet;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 
 public class TestRunResult
   implements ITestRunListener
 {
/*  35 */   private static final String LOG_TAG = TestRunResult.class.getSimpleName();
   private String mTestRunName;
/*  38 */   private Map<TestIdentifier, TestResult> mTestResults = new LinkedHashMap();
 
/*  40 */   private Map<String, String> mRunMetrics = new HashMap();
/*  41 */   private boolean mIsRunComplete = false;
/*  42 */   private long mElapsedTime = 0L;
 
/*  45 */   private int[] mStatusCounts = new int[TestResult.TestStatus.values().length];
 
/*  47 */   private boolean mIsCountDirty = true;
 
/*  49 */   private String mRunFailureError = null;
 
/*  51 */   private boolean mAggregateMetrics = false;
 
   public TestRunResult()
   {
/*  57 */     this.mTestRunName = "not started";
   }
 
   public void setAggregateMetrics(boolean metricAggregation) {
/*  61 */     this.mAggregateMetrics = metricAggregation;
   }
 
   public String getName()
   {
/*  68 */     return this.mTestRunName;
   }
 
   public Map<TestIdentifier, TestResult> getTestResults()
   {
/*  76 */     return this.mTestResults;
   }
 
   public Map<String, String> getRunMetrics()
   {
/*  83 */     return this.mRunMetrics;
   }
 
   public Set<TestIdentifier> getCompletedTests()
   {
/*  90 */     Set completedTests = new LinkedHashSet();
/*  91 */     for (Map.Entry testEntry : getTestResults().entrySet()) {
/*  92 */       if (!((TestResult)testEntry.getValue()).getStatus().equals(TestResult.TestStatus.INCOMPLETE)) {
/*  93 */         completedTests.add(testEntry.getKey());
       }
     }
/*  96 */     return completedTests;
   }
 
   public boolean isRunFailure()
   {
/* 103 */     return this.mRunFailureError != null;
   }
 
   public boolean isRunComplete()
   {
/* 110 */     return this.mIsRunComplete;
   }
 
   public void setRunComplete(boolean runComplete) {
/* 114 */     this.mIsRunComplete = runComplete;
   }
 
   public int getNumTestsInState(TestResult.TestStatus status)
   {
/* 121 */     if (this.mIsCountDirty)
     {
/* 123 */       for (int i = 0; i < this.mStatusCounts.length; i++) {
/* 124 */         this.mStatusCounts[i] = 0;
       }
 
/* 127 */       for (TestResult r : this.mTestResults.values()) {
/* 128 */         this.mStatusCounts[r.getStatus().ordinal()] += 1;
       }
/* 130 */       this.mIsCountDirty = false;
     }
/* 132 */     return this.mStatusCounts[status.ordinal()];
   }
 
   public int getNumTests()
   {
/* 139 */     return this.mTestResults.size();
   }
 
   public int getNumCompleteTests()
   {
/* 146 */     return getNumTests() - getNumTestsInState(TestResult.TestStatus.INCOMPLETE);
   }
 
   public boolean hasFailedTests()
   {
/* 153 */     return getNumAllFailedTests() > 0;
   }
 
   public int getNumAllFailedTests()
   {
/* 160 */     return getNumTestsInState(TestResult.TestStatus.FAILURE);
   }
 
   public long getElapsedTime()
   {
/* 167 */     return this.mElapsedTime;
   }
 
   public String getRunFailureMessage()
   {
/* 174 */     return this.mRunFailureError;
   }
 
   public void testRunStarted(String runName, int testCount)
   {
/* 180 */     this.mTestRunName = runName;
/* 181 */     this.mIsRunComplete = false;
/* 182 */     this.mRunFailureError = null;
   }
 
   public void testStarted(TestIdentifier test)
   {
/* 187 */     addTestResult(test, new TestResult());
   }
 
   private void addTestResult(TestIdentifier test, TestResult testResult) {
/* 191 */     this.mIsCountDirty = true;
/* 192 */     this.mTestResults.put(test, testResult);
   }
 
   private void updateTestResult(TestIdentifier test, TestResult.TestStatus status, String trace) {
/* 196 */     TestResult r = (TestResult)this.mTestResults.get(test);
/* 197 */     if (r == null) {
/* 198 */       Log.d(LOG_TAG, String.format("received test event without test start for %s", new Object[] { test }));
/* 199 */       r = new TestResult();
     }
/* 201 */     r.setStatus(status);
/* 202 */     r.setStackTrace(trace);
/* 203 */     addTestResult(test, r);
   }
 
   public void testFailed(TestIdentifier test, String trace)
   {
/* 208 */     updateTestResult(test, TestResult.TestStatus.FAILURE, trace);
   }
 
   public void testAssumptionFailure(TestIdentifier test, String trace)
   {
/* 213 */     updateTestResult(test, TestResult.TestStatus.ASSUMPTION_FAILURE, trace);
   }
 
   public void testIgnored(TestIdentifier test)
   {
/* 218 */     updateTestResult(test, TestResult.TestStatus.IGNORED, null);
   }
 
   public void testEnded(TestIdentifier test, Map<String, String> testMetrics)
   {
/* 223 */     TestResult result = (TestResult)this.mTestResults.get(test);
/* 224 */     if (result == null) {
/* 225 */       result = new TestResult();
     }
/* 227 */     if (result.getStatus().equals(TestResult.TestStatus.INCOMPLETE)) {
/* 228 */       result.setStatus(TestResult.TestStatus.PASSED);
     }
/* 230 */     result.setEndTime(System.currentTimeMillis());
/* 231 */     result.setMetrics(testMetrics);
/* 232 */     addTestResult(test, result);
   }
 
   public void testRunFailed(String errorMessage)
   {
/* 237 */     this.mRunFailureError = errorMessage;
   }
 
   public void testRunStopped(long elapsedTime)
   {
/* 242 */     this.mElapsedTime += elapsedTime;
/* 243 */     this.mIsRunComplete = true;
   }
 
   public void testRunEnded(long elapsedTime, Map<String, String> runMetrics)
   {
/* 248 */     if (this.mAggregateMetrics)
/* 249 */       for (Map.Entry<String, String> entry : runMetrics.entrySet()) {
/* 250 */         String existingValue = (String)this.mRunMetrics.get(entry.getKey());
/* 251 */         String combinedValue = combineValues(existingValue, (String)entry.getValue());
/* 252 */         this.mRunMetrics.put(entry.getKey(), combinedValue);
       }
     else {
/* 255 */       this.mRunMetrics.putAll(runMetrics);
     }
/* 257 */     this.mElapsedTime += elapsedTime;
/* 258 */     this.mIsRunComplete = true;
   }
 
   private String combineValues(String existingValue, String newValue)
   {
/* 269 */     if (existingValue != null)
       try {
/* 271 */         Long existingLong = Long.valueOf(Long.parseLong(existingValue));
/* 272 */         Long newLong = Long.valueOf(Long.parseLong(newValue));
/* 273 */         return Long.toString(existingLong.longValue() + newLong.longValue());
       }
       catch (NumberFormatException e)
       {
         try {
/* 278 */           Double existingDouble = Double.valueOf(Double.parseDouble(existingValue));
/* 279 */           Double newDouble = Double.valueOf(Double.parseDouble(newValue));
/* 280 */           return Double.toString(existingDouble.doubleValue() + newDouble.doubleValue());
         }
         catch (NumberFormatException c)
         {
         }
       }
/* 286 */     return newValue;
   }
 
   public String getTextSummary()
   {
/* 295 */     StringBuilder builder = new StringBuilder();
/* 296 */     builder.append(String.format("Total tests %d, ", new Object[] { Integer.valueOf(getNumTests()) }));
/* 297 */     for (TestResult.TestStatus status : TestResult.TestStatus.values()) {
/* 298 */       int count = getNumTestsInState(status);
 
/* 301 */       if (count > 0) {
/* 302 */         builder.append(String.format("%s %d, ", new Object[] { status.toString().toLowerCase(), Integer.valueOf(count) }));
       }
     }
/* 305 */     return builder.toString();
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.TestRunResult
 * JD-Core Version:    0.6.2
 */