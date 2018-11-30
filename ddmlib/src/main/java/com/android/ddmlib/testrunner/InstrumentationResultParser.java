 package com.android.ddmlib.testrunner;
 
 import com.android.ddmlib.Log;
 import com.android.ddmlib.MultiLineReceiver;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class InstrumentationResultParser extends MultiLineReceiver
 {
/*  81 */   private static final Set<String> KNOWN_KEYS = new HashSet();
   private final Collection<ITestRunListener> mTestListeners;
   private final String mTestRunName;
/* 157 */   private TestResult mCurrentTestResult = null;
 
/* 160 */   private TestResult mLastTestResult = null;
 
/* 163 */   private String mCurrentKey = null;
 
/* 166 */   private StringBuilder mCurrentValue = null;
 
/* 169 */   private boolean mTestStartReported = false;
 
/* 172 */   private boolean mTestRunFinished = false;
 
/* 175 */   private boolean mTestRunFailReported = false;
 
/* 178 */   private long mTestTime = 0L;
 
/* 181 */   private boolean mIsCancelled = false;
 
/* 184 */   private int mNumTestsRun = 0;
 
/* 187 */   private int mNumTestsExpected = 0;
 
/* 190 */   private boolean mInInstrumentationResultKey = false;
 
/* 196 */   private Map<String, String> mInstrumentationResultBundle = new HashMap();
 
/* 202 */   private Map<String, String> mTestMetrics = new HashMap();
   private static final String LOG_TAG = "InstrumentationResultParser";
   static final String NO_TEST_RESULTS_MSG = "No test results";
   static final String INCOMPLETE_TEST_ERR_MSG_PREFIX = "Test failed to run to completion";
   static final String INCOMPLETE_TEST_ERR_MSG_POSTFIX = "Check device logcat for details";
   static final String INCOMPLETE_RUN_ERR_MSG_PREFIX = "Test run failed to complete";
 
   public InstrumentationResultParser(String runName, Collection<ITestRunListener> listeners)
   {
/* 224 */     this.mTestRunName = runName;
/* 225 */     this.mTestListeners = new ArrayList(listeners);
   }
 
   public InstrumentationResultParser(String runName, ITestRunListener listener)
   {
/* 236 */     this(runName, Collections.singletonList(listener));
   }
 
   public void processNewLines(String[] lines)
   {
/* 246 */     for (String line : lines) {
/* 247 */       parse(line);
 
/* 249 */       Log.v("InstrumentationResultParser", line);
     }
   }
 
   private void parse(String line)
   {
/* 272 */     if (line.startsWith("INSTRUMENTATION_STATUS_CODE: "))
     {
/* 274 */       submitCurrentKeyValue();
/* 275 */       this.mInInstrumentationResultKey = false;
/* 276 */       parseStatusCode(line);
/* 277 */     } else if (line.startsWith("INSTRUMENTATION_STATUS: "))
     {
/* 279 */       submitCurrentKeyValue();
/* 280 */       this.mInInstrumentationResultKey = false;
/* 281 */       parseKey(line, "INSTRUMENTATION_STATUS: ".length());
/* 282 */     } else if (line.startsWith("INSTRUMENTATION_RESULT: "))
     {
/* 284 */       submitCurrentKeyValue();
/* 285 */       this.mInInstrumentationResultKey = true;
/* 286 */       parseKey(line, "INSTRUMENTATION_RESULT: ".length());
/* 287 */     } else if ((line.startsWith("INSTRUMENTATION_FAILED: ")) || (line.startsWith("INSTRUMENTATION_CODE: ")))
     {
/* 290 */       submitCurrentKeyValue();
/* 291 */       this.mInInstrumentationResultKey = false;
 
/* 293 */       this.mTestRunFinished = true;
     }
/* 295 */     else if (line.startsWith("Time: ")) {
/* 296 */       parseTime(line);
     }
/* 298 */     else if (this.mCurrentValue != null)
     {
/* 300 */       this.mCurrentValue.append("\r\n");
/* 301 */       this.mCurrentValue.append(line);
/* 302 */     } else if (!line.trim().isEmpty()) {
/* 303 */       Log.d("InstrumentationResultParser", "unrecognized line " + line);
     }
   }
 
   private void submitCurrentKeyValue()
   {
/* 312 */     if ((this.mCurrentKey != null) && (this.mCurrentValue != null)) {
/* 313 */       String statusValue = this.mCurrentValue.toString();
/* 314 */       if (this.mInInstrumentationResultKey) {
/* 315 */         if (!KNOWN_KEYS.contains(this.mCurrentKey))
/* 316 */           this.mInstrumentationResultBundle.put(this.mCurrentKey, statusValue);
/* 317 */         else if (this.mCurrentKey.equals("shortMsg"))
         {
/* 319 */           handleTestRunFailed(String.format("Instrumentation run failed due to '%1$s'", new Object[] { statusValue }));
         }
       }
       else {
/* 323 */         TestResult testInfo = getCurrentTestInfo();
 
/* 325 */         if (this.mCurrentKey.equals("class"))
/* 326 */           testInfo.mTestClass = statusValue.trim();
/* 327 */         else if (this.mCurrentKey.equals("test"))
/* 328 */           testInfo.mTestName = statusValue.trim();
/* 329 */         else if (this.mCurrentKey.equals("numtests")) {
           try {
/* 331 */             testInfo.mNumTests = Integer.valueOf(Integer.parseInt(statusValue));
           } catch (NumberFormatException e) {
/* 333 */             Log.w("InstrumentationResultParser", "Unexpected integer number of tests, received " + statusValue);
           }
         }
/* 336 */         else if (this.mCurrentKey.equals("Error"))
         {
/* 338 */           handleTestRunFailed(statusValue);
/* 339 */         } else if (this.mCurrentKey.equals("stack"))
/* 340 */           testInfo.mStackTrace = statusValue;
/* 341 */         else if (!KNOWN_KEYS.contains(this.mCurrentKey))
         {
/* 343 */           this.mTestMetrics.put(this.mCurrentKey, statusValue);
         }
       }
 
/* 347 */       this.mCurrentKey = null;
/* 348 */       this.mCurrentValue = null;
     }
   }
 
   private Map<String, String> getAndResetTestMetrics()
   {
/* 357 */     Map retVal = this.mTestMetrics;
/* 358 */     this.mTestMetrics = new HashMap();
/* 359 */     return retVal;
   }
 
   private TestResult getCurrentTestInfo() {
/* 363 */     if (this.mCurrentTestResult == null) {
/* 364 */       this.mCurrentTestResult = new TestResult();
     }
/* 366 */     return this.mCurrentTestResult;
   }
 
   private void clearCurrentTestInfo() {
/* 370 */     this.mLastTestResult = this.mCurrentTestResult;
/* 371 */     this.mCurrentTestResult = null;
   }
 
   private void parseKey(String line, int keyStartPos)
   {
/* 382 */     int endKeyPos = line.indexOf('=', keyStartPos);
/* 383 */     if (endKeyPos != -1) {
/* 384 */       this.mCurrentKey = line.substring(keyStartPos, endKeyPos).trim();
/* 385 */       parseValue(line, endKeyPos + 1);
     }
   }
 
   private void parseValue(String line, int valueStartPos)
   {
/* 396 */     this.mCurrentValue = new StringBuilder();
/* 397 */     this.mCurrentValue.append(line.substring(valueStartPos));
   }
 
   private void parseStatusCode(String line)
   {
/* 404 */     String value = line.substring("INSTRUMENTATION_STATUS_CODE: ".length()).trim();
/* 405 */     TestResult testInfo = getCurrentTestInfo();
/* 406 */     testInfo.mCode = Integer.valueOf(-1);
     try {
/* 408 */       testInfo.mCode = Integer.valueOf(Integer.parseInt(value));
     } catch (NumberFormatException e) {
/* 410 */       Log.w("InstrumentationResultParser", "Expected integer status code, received: " + value);
/* 411 */       testInfo.mCode = Integer.valueOf(-1);
     }
/* 413 */     if (testInfo.mCode.intValue() != 2)
     {
/* 415 */       reportResult(testInfo);
/* 416 */       clearCurrentTestInfo();
     }
   }
 
   public boolean isCancelled()
   {
/* 427 */     return this.mIsCancelled;
   }
 
   public void cancel()
   {
/* 434 */     this.mIsCancelled = true;
   }
 
   private void reportResult(TestResult testInfo)
   {
/* 444 */     if (!testInfo.isComplete()) {
/* 445 */       Log.w("InstrumentationResultParser", "invalid instrumentation status bundle " + testInfo.toString());
/* 446 */       return;
     }
/* 448 */     reportTestRunStarted(testInfo);
/* 449 */     TestIdentifier testId = new TestIdentifier(testInfo.mTestClass, testInfo.mTestName);
     Map metrics;
/* 452 */     switch (testInfo.mCode.intValue()) {
     case 1:
/* 454 */       for (ITestRunListener listener : this.mTestListeners) {
/* 455 */         listener.testStarted(testId);
       }
/* 457 */       break;
     case -2:
/* 459 */       metrics = getAndResetTestMetrics();
/* 460 */       for (ITestRunListener listener : this.mTestListeners) {
/* 461 */         listener.testFailed(testId, getTrace(testInfo));
/* 462 */         listener.testEnded(testId, metrics);
       }
/* 464 */       this.mNumTestsRun += 1;
/* 465 */       break;
     case -1:
/* 469 */       metrics = getAndResetTestMetrics();
/* 470 */       for (ITestRunListener listener : this.mTestListeners) {
/* 471 */         listener.testFailed(testId, getTrace(testInfo));
/* 472 */         listener.testEnded(testId, metrics);
       }
/* 474 */       this.mNumTestsRun += 1;
/* 475 */       break;
     case -3:
/* 477 */       metrics = getAndResetTestMetrics();
/* 478 */       for (ITestRunListener listener : this.mTestListeners) {
/* 479 */         listener.testStarted(testId);
/* 480 */         listener.testIgnored(testId);
/* 481 */         listener.testEnded(testId, metrics);
       }
/* 483 */       this.mNumTestsRun += 1;
/* 484 */       break;
     case -4:
/* 486 */       metrics = getAndResetTestMetrics();
/* 487 */       for (ITestRunListener listener : this.mTestListeners) {
/* 488 */         listener.testAssumptionFailure(testId, getTrace(testInfo));
/* 489 */         listener.testEnded(testId, metrics);
       }
/* 491 */       this.mNumTestsRun += 1;
/* 492 */       break;
     case 0:
/* 494 */       metrics = getAndResetTestMetrics();
/* 495 */       for (ITestRunListener listener : this.mTestListeners) {
/* 496 */         listener.testEnded(testId, metrics);
       }
/* 498 */       this.mNumTestsRun += 1;
/* 499 */       break;
     default:
/* 501 */       metrics = getAndResetTestMetrics();
/* 502 */       Log.e("InstrumentationResultParser", "Unknown status code received: " + testInfo.mCode);
/* 503 */       for (ITestRunListener listener : this.mTestListeners) {
/* 504 */         listener.testEnded(testId, metrics);
       }
/* 506 */       this.mNumTestsRun += 1;
     }
   }
 
   private void reportTestRunStarted(TestResult testInfo)
   {
/* 520 */     if ((!this.mTestStartReported) && (testInfo.mNumTests != null)) {
/* 521 */       for (ITestRunListener listener : this.mTestListeners) {
/* 522 */         listener.testRunStarted(this.mTestRunName, testInfo.mNumTests.intValue());
       }
/* 524 */       this.mNumTestsExpected = testInfo.mNumTests.intValue();
/* 525 */       this.mTestStartReported = true;
     }
   }
 
   private String getTrace(TestResult testInfo)
   {
/* 533 */     if (testInfo.mStackTrace != null) {
/* 534 */       return testInfo.mStackTrace;
     }
/* 536 */     Log.e("InstrumentationResultParser", "Could not find stack trace for failed test ");
/* 537 */     return new Throwable("Unknown failure").toString();
   }
 
   private void parseTime(String line)
   {
/* 545 */     Pattern timePattern = Pattern.compile(String.format("%s\\s*([\\d\\.]+)", new Object[] { "Time: " }));
 
/* 547 */     Matcher timeMatcher = timePattern.matcher(line);
/* 548 */     if (timeMatcher.find()) {
/* 549 */       String timeString = timeMatcher.group(1);
       try {
/* 551 */         float timeSeconds = Float.parseFloat(timeString);
/* 552 */         this.mTestTime = ((long)(timeSeconds * 1000.0F));
       } catch (NumberFormatException e) {
/* 554 */         Log.w("InstrumentationResultParser", String.format("Unexpected time format %1$s", new Object[] { line }));
       }
     } else {
/* 557 */       Log.w("InstrumentationResultParser", String.format("Unexpected time format %1$s", new Object[] { line }));
     }
   }
 
   public void handleTestRunFailed(String errorMsg)
   {
/* 566 */     errorMsg = errorMsg == null ? "Unknown error" : errorMsg;
/* 567 */     Log.i("InstrumentationResultParser", String.format("test run failed: '%1$s'", new Object[] { errorMsg }));
     TestIdentifier testId;
/* 568 */     if ((this.mLastTestResult != null) && (this.mLastTestResult.isComplete()) && (1 == this.mLastTestResult.mCode.intValue()))
     {
/* 574 */       testId = new TestIdentifier(this.mLastTestResult.mTestClass, this.mLastTestResult.mTestName);
 
/* 576 */       for (ITestRunListener listener : this.mTestListeners) {
/* 577 */         listener.testFailed(testId, String.format("%1$s. Reason: '%2$s'. %3$s", new Object[] { "Test failed to run to completion", errorMsg, "Check device logcat for details" }));
 
/* 580 */         listener.testEnded(testId, getAndResetTestMetrics());
       }
     }
/* 583 */     for (ITestRunListener listener : this.mTestListeners) {
/* 584 */       if (!this.mTestStartReported)
       {
/* 586 */         listener.testRunStarted(this.mTestRunName, 0);
       }
/* 588 */       listener.testRunFailed(errorMsg);
/* 589 */       listener.testRunEnded(this.mTestTime, this.mInstrumentationResultBundle);
     }
/* 591 */     this.mTestStartReported = true;
/* 592 */     this.mTestRunFailReported = true;
   }
 
   public void done()
   {
/* 600 */     super.done();
/* 601 */     if (!this.mTestRunFailReported)
/* 602 */       handleOutputDone();
   }
 
   private void handleOutputDone()
   {
/* 610 */     if ((!this.mTestStartReported) && (!this.mTestRunFinished))
     {
/* 612 */       handleTestRunFailed("No test results");
/* 613 */     } else if (this.mNumTestsExpected > this.mNumTestsRun) {
/* 614 */       String message = String.format("%1$s. Expected %2$d tests, received %3$d", new Object[] { "Test run failed to complete", Integer.valueOf(this.mNumTestsExpected), Integer.valueOf(this.mNumTestsRun) });
 
/* 617 */       handleTestRunFailed(message);
     } else {
/* 619 */       for (ITestRunListener listener : this.mTestListeners) {
/* 620 */         if (!this.mTestStartReported)
         {
/* 623 */           listener.testRunStarted(this.mTestRunName, 0);
         }
/* 625 */         listener.testRunEnded(this.mTestTime, this.mInstrumentationResultBundle);
       }
     }
   }
 
   static
   {
/*  83 */     KNOWN_KEYS.add("test");
/*  84 */     KNOWN_KEYS.add("class");
/*  85 */     KNOWN_KEYS.add("stack");
/*  86 */     KNOWN_KEYS.add("numtests");
/*  87 */     KNOWN_KEYS.add("Error");
/*  88 */     KNOWN_KEYS.add("shortMsg");
 
/*  90 */     KNOWN_KEYS.add("stream");
/*  91 */     KNOWN_KEYS.add("id");
/*  92 */     KNOWN_KEYS.add("current");
   }
 
   private static class TestResult
   {
/* 124 */     private Integer mCode = null;
/* 125 */     private String mTestName = null;
/* 126 */     private String mTestClass = null;
/* 127 */     private String mStackTrace = null;
/* 128 */     private Integer mNumTests = null;
 
     boolean isComplete()
     {
/* 132 */       return (this.mCode != null) && (this.mTestName != null) && (this.mTestClass != null);
     }
 
     public String toString()
     {
/* 138 */       StringBuilder output = new StringBuilder();
/* 139 */       if (this.mTestClass != null) {
/* 140 */         output.append(this.mTestClass);
/* 141 */         output.append('#');
       }
/* 143 */       if (this.mTestName != null) {
/* 144 */         output.append(this.mTestName);
       }
/* 146 */       if (output.length() > 0) {
/* 147 */         return output.toString();
       }
/* 149 */       return "unknown result";
     }
   }
 
   private static class Prefixes
   {
     private static final String STATUS = "INSTRUMENTATION_STATUS: ";
     private static final String STATUS_CODE = "INSTRUMENTATION_STATUS_CODE: ";
     private static final String STATUS_FAILED = "INSTRUMENTATION_FAILED: ";
     private static final String CODE = "INSTRUMENTATION_CODE: ";
     private static final String RESULT = "INSTRUMENTATION_RESULT: ";
     private static final String TIME_REPORT = "Time: ";
   }
 
   private static class StatusCodes
   {
     private static final int START = 1;
     private static final int IN_PROGRESS = 2;
     private static final int ASSUMPTION_FAILURE = -4;
     private static final int IGNORED = -3;
     private static final int FAILURE = -2;
     private static final int ERROR = -1;
     private static final int OK = 0;
   }
 
   private static class StatusKeys
   {
     private static final String TEST = "test";
     private static final String CLASS = "class";
     private static final String STACK = "stack";
     private static final String NUMTESTS = "numtests";
     private static final String ERROR = "Error";
     private static final String SHORTMSG = "shortMsg";
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.InstrumentationResultParser
 * JD-Core Version:    0.6.2
 */