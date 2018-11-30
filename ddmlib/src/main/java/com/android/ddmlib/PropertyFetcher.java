 package com.android.ddmlib;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.collect.Maps;
 import com.google.common.util.concurrent.SettableFuture;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 class PropertyFetcher
 {
   private static final String GETPROP_COMMAND = "getprop";
/*  35 */   private static final Pattern GETPROP_PATTERN = Pattern.compile("^\\[([^]]+)\\]\\:\\s*\\[(.*)\\]$");
   private static final int GETPROP_TIMEOUT_SEC = 2;
   private static final int EXPECTED_PROP_COUNT = 150;
/*  86 */   private final Map<String, String> mProperties = Maps.newHashMapWithExpectedSize(150);
   private final IDevice mDevice;
/*  89 */   private CacheState mCacheState = CacheState.UNPOPULATED;
/*  90 */   private final Map<String, SettableFuture<String>> mPendingRequests = Maps.newHashMapWithExpectedSize(4);
 
   public PropertyFetcher(IDevice device)
   {
/*  94 */     this.mDevice = device;
   }
 
   public synchronized Map<String, String> getProperties()
   {
/* 101 */     return this.mProperties;
   }
 
   public synchronized Future<String> getProperty(String name)
   {

     SettableFuture result;
/* 113 */     if (this.mCacheState.equals(CacheState.FETCHING)) {
/* 114 */       result = addPendingRequest(name);
/* 115 */     } else if (((this.mDevice.isOnline()) && (this.mCacheState.equals(CacheState.UNPOPULATED))) || (!isRoProp(name)))
     {
/* 117 */       result = addPendingRequest(name);
/* 118 */       this.mCacheState = CacheState.FETCHING;
/* 119 */       initiatePropertiesQuery();
     } else {
/* 121 */       result = SettableFuture.create();
 
/* 123 */       result.set(this.mProperties.get(name));
     }
/* 125 */     return result;
   }
 
   private SettableFuture<String> addPendingRequest(String name) {
/* 129 */     SettableFuture future = (SettableFuture)this.mPendingRequests.get(name);
/* 130 */     if (future == null) {
/* 131 */       future = SettableFuture.create();
/* 132 */       this.mPendingRequests.put(name, future);
     }
/* 134 */     return future;
   }
 
   private void initiatePropertiesQuery() {
/* 138 */     String threadName = String.format("query-prop-%s", new Object[] { this.mDevice.getSerialNumber() });
/* 139 */     Thread propThread = new Thread(threadName)
     {
       public void run() {
         try {
/* 143 */           PropertyFetcher.GetPropReceiver propReceiver = new PropertyFetcher.GetPropReceiver();
/* 144 */           PropertyFetcher.this.mDevice.executeShellCommand("getprop", propReceiver, 2L, TimeUnit.SECONDS);
 
/* 146 */           PropertyFetcher.this.populateCache(propReceiver.getCollectedProperties());
         } catch (Exception e) {
/* 148 */           PropertyFetcher.this.handleException(e);
         }
       }
     };
/* 152 */     propThread.setDaemon(true);
/* 153 */     propThread.start();
   }
 
   private synchronized void populateCache(Map<String, String> props) {
/* 157 */     this.mCacheState = (props.isEmpty() ? CacheState.UNPOPULATED : CacheState.POPULATED);
/* 158 */     if (!props.isEmpty()) {
/* 159 */       this.mProperties.putAll(props);
     }
/* 161 */     for (Map.Entry entry : this.mPendingRequests.entrySet()) {
/* 162 */       ((SettableFuture)entry.getValue()).set(this.mProperties.get(entry.getKey()));
     }
/* 164 */     this.mPendingRequests.clear();
   }
 
   private synchronized void handleException(Exception e) {
/* 168 */     this.mCacheState = CacheState.UNPOPULATED;
/* 169 */     Log.w("PropertyFetcher", String.format("%s getting properties for device %s: %s", new Object[] { e.getClass().getSimpleName(), this.mDevice.getSerialNumber(), e.getMessage() }));
 
/* 173 */     for (Map.Entry entry : this.mPendingRequests.entrySet()) {
/* 174 */       ((SettableFuture)entry.getValue()).setException(e);
     }
/* 176 */     this.mPendingRequests.clear();
   }
 
   @Deprecated
   public synchronized boolean arePropertiesSet()
   {
/* 186 */     return CacheState.POPULATED.equals(this.mCacheState);
   }
 
   private static boolean isRoProp(String propName) {
/* 190 */     return propName.startsWith("ro.");
   }
 
   @VisibleForTesting
   static class GetPropReceiver extends MultiLineReceiver
   {
/*  49 */     private final Map<String, String> mCollectedProperties = Maps.newHashMapWithExpectedSize(150);
 
     public void processNewLines(String[] lines)
     {
/*  59 */       for (String line : lines)
/*  60 */         if ((!line.isEmpty()) && (!line.startsWith("#")))
         {
/*  64 */           Matcher m = PropertyFetcher.GETPROP_PATTERN.matcher(line);
/*  65 */           if (m.matches()) {
/*  66 */             String label = m.group(1);
/*  67 */             String value = m.group(2);
 
/*  69 */             if (!label.isEmpty())
/*  70 */               this.mCollectedProperties.put(label, value);
           }
         }
     }
 
     public boolean isCancelled()
     {
/*  78 */       return false;
     }
 
     Map<String, String> getCollectedProperties() {
/*  82 */       return this.mCollectedProperties;
     }
   }
 
   private static enum CacheState
   {
/*  40 */     UNPOPULATED, FETCHING, POPULATED;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.PropertyFetcher
 * JD-Core Version:    0.6.2
 */