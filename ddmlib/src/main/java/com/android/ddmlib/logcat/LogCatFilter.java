 package com.android.ddmlib.logcat;
 
 import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import java.util.regex.PatternSyntaxException;
 
 public final class LogCatFilter
 {
   private static final String PID_KEYWORD = "pid:";
   private static final String APP_KEYWORD = "app:";
   private static final String TAG_KEYWORD = "tag:";
   private static final String TEXT_KEYWORD = "text:";
   private final String mName;
   private final String mTag;
   private final String mText;
   private final String mPid;
   private final String mAppName;
   private final Log.LogLevel mLogLevel;
   private boolean mCheckPid;
   private boolean mCheckAppName;
   private boolean mCheckTag;
   private boolean mCheckText;
   private Pattern mAppNamePattern;
   private Pattern mTagPattern;
   private Pattern mTextPattern;
 
   public LogCatFilter(String name, String tag, String text, String pid, String appName, Log.LogLevel logLevel)
   {
/*  67 */     this.mName = name.trim();
/*  68 */     this.mTag = tag.trim();
/*  69 */     this.mText = text.trim();
/*  70 */     this.mPid = pid.trim();
/*  71 */     this.mAppName = appName.trim();
/*  72 */     this.mLogLevel = logLevel;
 
/*  74 */     this.mCheckPid = (!this.mPid.isEmpty());
 
/*  76 */     if (!this.mAppName.isEmpty()) {
       try {
/*  78 */         this.mAppNamePattern = Pattern.compile(this.mAppName, getPatternCompileFlags(this.mAppName));
/*  79 */         this.mCheckAppName = true;
       } catch (PatternSyntaxException e) {
/*  81 */         this.mCheckAppName = false;
       }
     }
 
/*  85 */     if (!this.mTag.isEmpty()) {
       try {
/*  87 */         this.mTagPattern = Pattern.compile(this.mTag, getPatternCompileFlags(this.mTag));
/*  88 */         this.mCheckTag = true;
       } catch (PatternSyntaxException e) {
/*  90 */         this.mCheckTag = false;
       }
     }
 
/*  94 */     if (!this.mText.isEmpty())
       try {
/*  96 */         this.mTextPattern = Pattern.compile(this.mText, getPatternCompileFlags(this.mText));
/*  97 */         this.mCheckText = true;
       } catch (PatternSyntaxException e) {
/*  99 */         this.mCheckText = false;
       }
   }
 
   private int getPatternCompileFlags(String regex)
   {
/* 111 */     for (char c : regex.toCharArray()) {
/* 112 */       if (Character.isUpperCase(c)) {
/* 113 */         return 0;
       }
     }
 
/* 117 */     return 2;
   }
 
   public static List<LogCatFilter> fromString(String query, Log.LogLevel minLevel)
   {
/* 131 */     List filterSettings = new ArrayList();
 
/* 133 */     for (String s : query.trim().split(" ")) {
/* 134 */       String tag = "";
/* 135 */       String text = "";
/* 136 */       String pid = "";
/* 137 */       String app = "";
 
/* 139 */       if (s.startsWith("pid:"))
/* 140 */         pid = s.substring("pid:".length());
/* 141 */       else if (s.startsWith("app:"))
/* 142 */         app = s.substring("app:".length());
/* 143 */       else if (s.startsWith("tag:")) {
/* 144 */         tag = s.substring("tag:".length());
       }
/* 146 */       else if (s.startsWith("text:"))
/* 147 */         text = s.substring("text:".length());
       else {
/* 149 */         text = s;
       }
 
/* 152 */       filterSettings.add(new LogCatFilter("livefilter-" + s, tag, text, pid, app, minLevel));
     }
 
/* 156 */     return filterSettings;
   }
 
   public String getName()
   {
/* 161 */     return this.mName;
   }
 
   public String getTag()
   {
/* 166 */     return this.mTag;
   }
 
   public String getText()
   {
/* 171 */     return this.mText;
   }
 
   public String getPid()
   {
/* 176 */     return this.mPid;
   }
 
   public String getAppName()
   {
/* 181 */     return this.mAppName;
   }
 
   public Log.LogLevel getLogLevel()
   {
/* 186 */     return this.mLogLevel;
   }
 
   public boolean matches(LogCatMessage m)
   {
/* 196 */     if (m.getLogLevel().getPriority() < this.mLogLevel.getPriority()) {
/* 197 */       return false;
     }
 
/* 202 */     if ((this.mCheckPid) && (!Integer.toString(m.getPid()).equals(this.mPid))) {
/* 203 */       return false;
     }
 
/* 207 */     if (this.mCheckAppName) {
/* 208 */       Matcher matcher = this.mAppNamePattern.matcher(m.getAppName());
/* 209 */       if (!matcher.find()) {
/* 210 */         return false;
       }
 
     }
 
/* 215 */     if (this.mCheckTag) {
/* 216 */       Matcher matcher = this.mTagPattern.matcher(m.getTag());
/* 217 */       if (!matcher.find()) {
/* 218 */         return false;
       }
     }
 
/* 222 */     if (this.mCheckText) {
/* 223 */       Matcher matcher = this.mTextPattern.matcher(m.getMessage());
/* 224 */       if (!matcher.find()) {
/* 225 */         return false;
       }
     }
 
/* 229 */     return true;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatFilter
 * JD-Core Version:    0.6.2
 */