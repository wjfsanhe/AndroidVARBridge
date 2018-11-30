 package com.android.ddmlib.testrunner;
 
 import com.android.ddmlib.Log;
 import com.android.ddmlib.Log.LogLevel;
 import com.google.common.collect.ImmutableMap;
 import java.io.BufferedOutputStream;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.TimeZone;
 import com.android.ddmlib.utils.KXmlSerializer;
 
 public class XmlTestRunListener
   implements ITestRunListener
 {
   private static final String LOG_TAG = "XmlResultReporter";
   private static final String TEST_RESULT_FILE_SUFFIX = ".xml";
   private static final String TEST_RESULT_FILE_PREFIX = "test_result_";
   private static final String TESTSUITE = "testsuite";
   private static final String TESTCASE = "testcase";
   private static final String ERROR = "error";
   private static final String FAILURE = "failure";
   private static final String SKIPPED_TAG = "skipped";
   private static final String ATTR_NAME = "name";
   private static final String ATTR_TIME = "time";
   private static final String ATTR_ERRORS = "errors";
   private static final String ATTR_FAILURES = "failures";
   private static final String ATTR_SKIPPED = "skipped";
   private static final String ATTR_ASSERTIOMS = "assertions";
   private static final String ATTR_TESTS = "tests";
   private static final String PROPERTIES = "properties";
   private static final String PROPERTY = "property";
   private static final String ATTR_CLASSNAME = "classname";
   private static final String TIMESTAMP = "timestamp";
   private static final String HOSTNAME = "hostname";
/*  76 */   private static final String ns = null;
 
/*  78 */   private String mHostName = "localhost";
 
/*  80 */   private File mReportDir = new File(System.getProperty("java.io.tmpdir"));
 
/*  82 */   private String mReportPath = "";
 
/*  84 */   private TestRunResult mRunResult = new TestRunResult();
 
   public void setReportDir(File file)
   {
/*  90 */     this.mReportDir = file;
   }
 
   public void setHostName(String hostName) {
/*  94 */     this.mHostName = hostName;
   }
 
   public TestRunResult getRunResult()
   {
/* 102 */     return this.mRunResult;
   }
 
   public void testRunStarted(String runName, int numTests)
   {
/* 107 */     this.mRunResult = new TestRunResult();
/* 108 */     this.mRunResult.testRunStarted(runName, numTests);
   }
 
   public void testStarted(TestIdentifier test)
   {
/* 113 */     this.mRunResult.testStarted(test);
   }
 
   public void testFailed(TestIdentifier test, String trace)
   {
/* 118 */     this.mRunResult.testFailed(test, trace);
   }
 
   public void testAssumptionFailure(TestIdentifier test, String trace)
   {
/* 123 */     this.mRunResult.testAssumptionFailure(test, trace);
   }
 
   public void testIgnored(TestIdentifier test)
   {
/* 128 */     this.mRunResult.testIgnored(test);
   }
 
   public void testEnded(TestIdentifier test, Map<String, String> testMetrics)
   {
/* 133 */     this.mRunResult.testEnded(test, testMetrics);
   }
 
   public void testRunFailed(String errorMessage)
   {
/* 138 */     this.mRunResult.testRunFailed(errorMessage);
   }
 
   public void testRunStopped(long elapsedTime)
   {
/* 143 */     this.mRunResult.testRunStopped(elapsedTime);
   }
 
   public void testRunEnded(long elapsedTime, Map<String, String> runMetrics)
   {
/* 148 */     this.mRunResult.testRunEnded(elapsedTime, runMetrics);
/* 149 */     generateDocument(this.mReportDir, elapsedTime);
   }
 
   private void generateDocument(File reportDir, long elapsedTime)
   {
/* 156 */     String timestamp = getTimestamp();
 
/* 158 */     OutputStream stream = null;
     try {
/* 160 */       stream = createOutputResultStream(reportDir);
/* 161 */       KXmlSerializer serializer = new KXmlSerializer();
/* 162 */       serializer.setOutput(stream, "UTF-8");
/* 163 */       serializer.startDocument("UTF-8", null);
/* 164 */       serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
 
/* 167 */       printTestResults(serializer, timestamp, elapsedTime);
/* 168 */       serializer.endDocument();
/* 169 */       String msg = String.format("XML test result file generated at %s. %s", new Object[] { getAbsoluteReportPath(), this.mRunResult.getTextSummary() });
 
/* 171 */       Log.logAndDisplay(Log.LogLevel.INFO, "XmlResultReporter", msg);
     } catch (IOException e) {
/* 173 */       Log.e("XmlResultReporter", "Failed to generate report data");
     }
     finally {
/* 176 */       if (stream != null)
         try {
/* 178 */           stream.close();
         }
         catch (IOException ignored) {
         }
     }
   }
 
   private String getAbsoluteReportPath() {
/* 186 */     return this.mReportPath;
   }
 
   String getTimestamp()
   {
/* 193 */     SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
 
/* 195 */     TimeZone gmt = TimeZone.getTimeZone("UTC");
/* 196 */     dateFormat.setTimeZone(gmt);
/* 197 */     dateFormat.setLenient(true);
/* 198 */     String timestamp = dateFormat.format(new Date());
/* 199 */     return timestamp;
   }
 
   protected File getResultFile(File reportDir)
     throws IOException
   {
/* 209 */     File reportFile = File.createTempFile("test_result_", ".xml", reportDir);
 
/* 211 */     Log.i("XmlResultReporter", String.format("Created xml report file at %s", new Object[] { reportFile.getAbsolutePath() }));
 
/* 214 */     return reportFile;
   }
 
   OutputStream createOutputResultStream(File reportDir)
     throws IOException
   {
/* 221 */     File reportFile = getResultFile(reportDir);
/* 222 */     this.mReportPath = reportFile.getAbsolutePath();
/* 223 */     return new BufferedOutputStream(new FileOutputStream(reportFile));
   }
 
   protected String getTestSuiteName() {
/* 227 */     return this.mRunResult.getName();
   }
 
   void printTestResults(KXmlSerializer serializer, String timestamp, long elapsedTime) throws IOException
   {
/* 232 */     serializer.startTag(ns, "testsuite");
/* 233 */     String name = getTestSuiteName();
/* 234 */     if (name != null) {
/* 235 */       serializer.attribute(ns, "name", name);
     }
/* 237 */     serializer.attribute(ns, "tests", Integer.toString(this.mRunResult.getNumTests()));
/* 238 */     serializer.attribute(ns, "failures", Integer.toString(this.mRunResult.getNumAllFailedTests()));
 
/* 241 */     serializer.attribute(ns, "errors", "0");
/* 242 */     serializer.attribute(ns, "skipped", Integer.toString(this.mRunResult.getNumTestsInState(TestResult.TestStatus.IGNORED)));
 
/* 245 */     serializer.attribute(ns, "time", Double.toString(elapsedTime / 1000.0D));
/* 246 */     serializer.attribute(ns, "timestamp", timestamp);
/* 247 */     serializer.attribute(ns, "hostname", this.mHostName);
 
/* 249 */     serializer.startTag(ns, "properties");
/* 250 */     for (Map.Entry entry : getPropertiesAttributes().entrySet()) {
/* 251 */       serializer.startTag(ns, "property");
/* 252 */       serializer.attribute(ns, "name", (String)entry.getKey());
/* 253 */       serializer.attribute(ns, "value", (String)entry.getValue());
/* 254 */       serializer.endTag(ns, "property");
     }
/* 256 */     serializer.endTag(ns, "properties");
 
/* 258 */     Map<TestIdentifier, TestResult> testResults = this.mRunResult.getTestResults();
/* 259 */     for (Map.Entry<TestIdentifier, TestResult> testEntry : testResults.entrySet()) {
/* 260 */       print(serializer, (TestIdentifier)testEntry.getKey(), (TestResult)testEntry.getValue());
     }
 
/* 263 */     serializer.endTag(ns, "testsuite");
   }
 
   protected Map<String, String> getPropertiesAttributes()
   {
/* 271 */     return ImmutableMap.of();
   }
 
   protected String getTestName(TestIdentifier testId) {
/* 275 */     return testId.getTestName();
   }
 
   void print(KXmlSerializer serializer, TestIdentifier testId, TestResult testResult)
     throws IOException
   {
/* 281 */     serializer.startTag(ns, "testcase");
/* 282 */     serializer.attribute(ns, "name", getTestName(testId));
/* 283 */     serializer.attribute(ns, "classname", testId.getClassName());
/* 284 */     long elapsedTimeMs = testResult.getEndTime() - testResult.getStartTime();
/* 285 */     serializer.attribute(ns, "time", Double.toString(elapsedTimeMs / 1000.0D));
 
/* 287 */     switch (testResult.getStatus().ordinal()) {
     case 1:
/* 289 */       printFailedTest(serializer, "failure", testResult.getStackTrace());
/* 290 */       break;
     case 2:
/* 292 */       printFailedTest(serializer, "skipped", testResult.getStackTrace());
/* 293 */       break;
     case 3:
/* 295 */       serializer.startTag(ns, "skipped");
/* 296 */       serializer.endTag(ns, "skipped");
     }
 
/* 300 */     serializer.endTag(ns, "testcase");
   }
 
   private void printFailedTest(KXmlSerializer serializer, String tag, String stack) throws IOException
   {
/* 305 */     serializer.startTag(ns, tag);
 
/* 313 */     serializer.text(sanitize(stack));
/* 314 */     serializer.endTag(ns, tag);
   }
 
   private String sanitize(String text)
   {
/* 321 */     return text.replace("", "<\\0>");
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.XmlTestRunListener
 * JD-Core Version:    0.6.2
 */