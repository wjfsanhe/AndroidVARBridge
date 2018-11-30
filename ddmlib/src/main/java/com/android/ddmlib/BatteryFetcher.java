 package com.android.ddmlib;
 
 import com.google.common.util.concurrent.SettableFuture;
 import java.io.IOException;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 class BatteryFetcher
 {
   private static final String LOG_TAG = "BatteryFetcher";
   private static final long FETCH_BACKOFF_MS = 5000L;
   private static final long BATTERY_TIMEOUT = 2000L;
/* 133 */   private Integer mBatteryLevel = null;
   private final IDevice mDevice;
/* 135 */   private long mLastSuccessTime = 0L;
/* 136 */   private SettableFuture<Integer> mPendingRequest = null;
 
   public BatteryFetcher(IDevice device) {
/* 139 */     this.mDevice = device;
   }
 
   public synchronized Future<Integer> getBattery(long freshness, TimeUnit timeUnit)
   {

     SettableFuture result;
/* 151 */     if ((this.mBatteryLevel == null) || (isFetchRequired(freshness, timeUnit))) {
/* 152 */       if (this.mPendingRequest == null)
       {
/* 154 */         this.mPendingRequest = SettableFuture.create();
/* 155 */         initiateBatteryQuery();
       }
 
/* 160 */       result = this.mPendingRequest;
     }
     else {
/* 163 */       result = SettableFuture.create();
/* 164 */       result.set(this.mBatteryLevel);
     }
/* 166 */     return result;
   }
 
   private boolean isFetchRequired(long freshness, TimeUnit timeUnit) {
/* 170 */     long freshnessMs = timeUnit.toMillis(freshness);
/* 171 */     return System.currentTimeMillis() - this.mLastSuccessTime > freshnessMs;
   }
 
   private void initiateBatteryQuery() {
/* 175 */     String threadName = String.format("query-battery-%s", new Object[] { this.mDevice.getSerialNumber() });
/* 176 */     Thread fetchThread = new Thread(threadName)
     {
       public void run() {
/* 179 */         Exception exception = null;
         try
         {
/* 182 */           BatteryFetcher.SysFsBatteryLevelReceiver sysBattReceiver = new BatteryFetcher.SysFsBatteryLevelReceiver();
/* 183 */           BatteryFetcher.this.mDevice.executeShellCommand("cat /sys/class/power_supply/*/capacity", sysBattReceiver, 2000L, TimeUnit.MILLISECONDS);
 
/* 185 */           if (!BatteryFetcher.this.setBatteryLevel(sysBattReceiver.getBatteryLevel()))
           {
/* 187 */             BatteryFetcher.BatteryReceiver receiver = new BatteryFetcher.BatteryReceiver();
/* 188 */             BatteryFetcher.this.mDevice.executeShellCommand("dumpsys battery", receiver, 2000L, TimeUnit.MILLISECONDS);
 
/* 190 */             if (BatteryFetcher.this.setBatteryLevel(receiver.getBatteryLevel())) {
/* 191 */               return;
             }
           }
/* 194 */           exception = new IOException("Unrecognized response to battery level queries");
         } catch (TimeoutException e) {
/* 196 */           exception = e;
         } catch (AdbCommandRejectedException e) {
/* 198 */           exception = e;
         } catch (ShellCommandUnresponsiveException e) {
/* 200 */           exception = e;
         } catch (IOException e) {
/* 202 */           exception = e;
         }
/* 204 */         BatteryFetcher.this.handleBatteryLevelFailure(exception);
       }
     };
/* 207 */     fetchThread.setDaemon(true);
/* 208 */     fetchThread.start();
   }
 
   private synchronized boolean setBatteryLevel(Integer batteryLevel) {
/* 212 */     if (batteryLevel == null) {
/* 213 */       return false;
     }
/* 215 */     this.mLastSuccessTime = System.currentTimeMillis();
/* 216 */     this.mBatteryLevel = batteryLevel;
/* 217 */     if (this.mPendingRequest != null) {
/* 218 */       this.mPendingRequest.set(this.mBatteryLevel);
     }
/* 220 */     this.mPendingRequest = null;
/* 221 */     return true;
   }
 
   private synchronized void handleBatteryLevelFailure(Exception e) {
/* 225 */     Log.w("BatteryFetcher", String.format("%s getting battery level for device %s: %s", new Object[] { e.getClass().getSimpleName(), this.mDevice.getSerialNumber(), e.getMessage() }));
 
/* 228 */     if ((this.mPendingRequest != null) && 
/* 229 */       (!this.mPendingRequest.setException(e)))
     {
/* 231 */       Log.e("BatteryFetcher", "Future.setException failed");
/* 232 */       this.mPendingRequest.set(null);
     }
 
/* 235 */     this.mPendingRequest = null;
   }
 
   private static final class BatteryReceiver extends MultiLineReceiver
   {
/*  86 */     private static final Pattern BATTERY_LEVEL = Pattern.compile("\\s*level: (\\d+)");
/*  87 */     private static final Pattern SCALE = Pattern.compile("\\s*scale: (\\d+)");
 
/*  89 */     private Integer mBatteryLevel = null;
/*  90 */     private Integer mBatteryScale = null;
 
     public Integer getBatteryLevel()
     {
/*  97 */       if ((this.mBatteryLevel != null) && (this.mBatteryScale != null)) {
/*  98 */         return Integer.valueOf(this.mBatteryLevel.intValue() * 100 / this.mBatteryScale.intValue());
       }
/* 100 */       return null;
     }
 
     public void processNewLines(String[] lines)
     {
/* 105 */       for (String line : lines) {
/* 106 */         Matcher batteryMatch = BATTERY_LEVEL.matcher(line);
/* 107 */         if (batteryMatch.matches()) {
           try {
/* 109 */             this.mBatteryLevel = Integer.valueOf(Integer.parseInt(batteryMatch.group(1)));
           } catch (NumberFormatException e) {
/* 111 */             Log.w("BatteryFetcher", String.format("Failed to parse %s as an integer", new Object[] { batteryMatch.group(1) }));
           }
         }
 
/* 115 */         Matcher scaleMatch = SCALE.matcher(line);
/* 116 */         if (scaleMatch.matches())
           try {
/* 118 */             this.mBatteryScale = Integer.valueOf(Integer.parseInt(scaleMatch.group(1)));
           } catch (NumberFormatException e) {
/* 120 */             Log.w("BatteryFetcher", String.format("Failed to parse %s as an integer", new Object[] { batteryMatch.group(1) }));
           }
       }
     }
 
     public boolean isCancelled()
     {
/* 129 */       return false;
     }
   }
 
   static final class SysFsBatteryLevelReceiver extends MultiLineReceiver
   {
/*  43 */     private static final Pattern BATTERY_LEVEL = Pattern.compile("^(\\d+)[.\\s]*");
/*  44 */     private Integer mBatteryLevel = null;
 
     public Integer getBatteryLevel()
     {
/*  52 */       return this.mBatteryLevel;
     }
 
     public boolean isCancelled()
     {
/*  57 */       return false;
     }
 
     public void processNewLines(String[] lines)
     {
/*  62 */       for (String line : lines) {
/*  63 */         Matcher batteryMatch = BATTERY_LEVEL.matcher(line);
/*  64 */         if (batteryMatch.matches())
/*  65 */           if (this.mBatteryLevel == null) {
/*  66 */             this.mBatteryLevel = Integer.valueOf(Integer.parseInt(batteryMatch.group(1)));
           }
           else {
/*  69 */             Integer tmpLevel = Integer.valueOf(Integer.parseInt(batteryMatch.group(1)));
/*  70 */             if (!this.mBatteryLevel.equals(tmpLevel))
/*  71 */               Log.w("BatteryFetcher", String.format("Multiple lines matched with different value; Original: %s, Current: %s (keeping original)", new Object[] { this.mBatteryLevel.toString(), tmpLevel.toString() }));
           }
       }
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.BatteryFetcher
 * JD-Core Version:    0.6.2
 */