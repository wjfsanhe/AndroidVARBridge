 package com.android.ddmlib.logcat;
 
 import com.android.ddmlib.AdbCommandRejectedException;
 import com.android.ddmlib.IDevice;
 import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
 import com.android.ddmlib.MultiLineReceiver;
 import com.android.ddmlib.ShellCommandUnresponsiveException;
 import com.android.ddmlib.TimeoutException;
 import java.io.IOException;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicBoolean;
 
 public class LogCatReceiverTask
   implements Runnable
 {
   private static final String LOGCAT_COMMAND = "logcat -v long";
   private static final int DEVICE_POLL_INTERVAL_MSEC = 1000;
/*  40 */   private static final LogCatMessage sDeviceDisconnectedMsg = new LogCatMessage(Log.LogLevel.ERROR, "Device disconnected: 1");
 
/*  42 */   private static final LogCatMessage sConnectionTimeoutMsg = new LogCatMessage(Log.LogLevel.ERROR, "LogCat Connection timed out");
 
/*  44 */   private static final LogCatMessage sConnectionErrorMsg = new LogCatMessage(Log.LogLevel.ERROR, "LogCat Connection error");
   private final IDevice mDevice;
   private final LogCatOutputReceiver mReceiver;
   private final LogCatMessageParser mParser;
   private final AtomicBoolean mCancelled;
/*  52 */   private final Set<LogCatListener> mListeners = new HashSet();
 
   public LogCatReceiverTask(IDevice device)
   {
/*  56 */     this.mDevice = device;
 
/*  58 */     this.mReceiver = new LogCatOutputReceiver();
/*  59 */     this.mParser = new LogCatMessageParser();
/*  60 */     this.mCancelled = new AtomicBoolean();
   }
 
   public void run()
   {
/*  66 */     while (!this.mDevice.isOnline()) {
       try {
/*  68 */         Thread.sleep(1000L);
       } catch (InterruptedException e) {
/*  70 */         return;
       }
     }
     try
     {
/*  75 */       this.mDevice.executeShellCommand("logcat -v long", this.mReceiver, 0);
     } catch (TimeoutException e) {
/*  77 */       notifyListeners(Collections.singletonList(sConnectionTimeoutMsg));
     } catch (AdbCommandRejectedException ignored) {
     }
     catch (ShellCommandUnresponsiveException ignored) {
     }
     catch (IOException e) {
/*  83 */       notifyListeners(Collections.singletonList(sConnectionErrorMsg));
     }
 
/*  86 */     notifyListeners(Collections.singletonList(sDeviceDisconnectedMsg));
   }
 
   public void stop() {
/*  90 */     this.mCancelled.set(true);
   }
 
   public synchronized void addLogCatListener(LogCatListener l)
   {
/* 120 */     this.mListeners.add(l);
   }
 
   public synchronized void removeLogCatListener(LogCatListener l) {
/* 124 */     this.mListeners.remove(l);
   }
 
   private synchronized void notifyListeners(List<LogCatMessage> messages) {
/* 128 */     for (LogCatListener l : this.mListeners)
/* 129 */       l.log(messages);
   }
 
   private class LogCatOutputReceiver extends MultiLineReceiver
   {
     public LogCatOutputReceiver()
     {
/*  95 */       setTrimLine(false);
     }
 
     public boolean isCancelled()
     {
/* 101 */       return LogCatReceiverTask.this.mCancelled.get();
     }
 
     public void processNewLines(String[] lines)
     {
/* 106 */       if (!LogCatReceiverTask.this.mCancelled.get())
/* 107 */         processLogLines(lines);
     }
 
     private void processLogLines(String[] lines)
     {
/* 112 */       List newMessages = LogCatReceiverTask.this.mParser.processLogLines(lines, LogCatReceiverTask.this.mDevice);
/* 113 */       if (!newMessages.isEmpty())
/* 114 */         LogCatReceiverTask.this.notifyListeners(newMessages);
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.logcat.LogCatReceiverTask
 * JD-Core Version:    0.6.2
 */