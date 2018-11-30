 package com.android.ddmlib;
 
 import com.google.common.base.Joiner;
 import com.google.common.base.Throwables;
 import com.google.common.util.concurrent.ListenableFuture;
 import com.google.common.util.concurrent.SettableFuture;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.UnknownHostException;
 import java.security.InvalidParameterException;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.TimeoutException;


 public final class AndroidDebugBridge
 {
/*   54 */   private static final AdbVersion MIN_ADB_VERSION = AdbVersion.parseFrom("1.0.20");
   private static final String ADB = "adb";
   private static final String DDMS = "ddms";
   private static final String SERVER_PORT_ENV_VAR = "ANDROID_ADB_SERVER_PORT";
   static final String DEFAULT_ADB_HOST = "127.0.0.1";
   static final int DEFAULT_ADB_PORT = 5040;
/*   65 */   private static int sAdbServerPort = 0;
   private static InetAddress sHostAddr;
   private static InetSocketAddress sSocketAddr;
   private static AndroidDebugBridge sThis;
/*   71 */   private static boolean sInitialized = false;
   private static boolean sClientSupport;
/*   75 */   private String mAdbOsLocation = null;//  /system/bin/adb
   private boolean mVersionCheck;
/*   79 */   private boolean mStarted = false;
   private DeviceMonitor mDeviceMonitor;
/*   83 */   private static final ArrayList<IDebugBridgeChangeListener> sBridgeListeners = new ArrayList();
 
/*   85 */   private static final ArrayList<IDeviceChangeListener> sDeviceListeners = new ArrayList();
 
/*   87 */   private static final ArrayList<IClientChangeListener> sClientListeners = new ArrayList();
 
/*   91 */   private static final Object sLock = sBridgeListeners;
 
   public static synchronized void initIfNeeded(boolean clientSupport)
   {
/*  168 */     if (sInitialized) {
/*  169 */       return;
     }
 
/*  172 */     init(clientSupport);
   }
 
   public static synchronized void init(boolean clientSupport)
   {
/*  201 */     if (sInitialized) {
/*  202 */       throw new IllegalStateException("AndroidDebugBridge.init() has already been called.");
     }
/*  204 */     sInitialized = true;
/*  205 */     sClientSupport = clientSupport;
 
/*  208 */     initAdbSocketAddr();
 
/*  210 */     MonitorThread monitorThread = MonitorThread.createInstance();
/*  211 */     monitorThread.start();
 
/*  213 */     HandleHello.register(monitorThread);
/*  214 */     HandleAppName.register(monitorThread);
/*  215 */     HandleTest.register(monitorThread);
/*  216 */     HandleThread.register(monitorThread);
/*  217 */     HandleHeap.register(monitorThread);
/*  218 */     HandleWait.register(monitorThread);
/*  219 */     HandleProfiling.register(monitorThread);
/*  220 */     HandleNativeHeap.register(monitorThread);
/*  221 */     HandleViewDebug.register(monitorThread);
   }
 
   public static synchronized void terminate()
   {
/*  229 */     if ((sThis != null) && (sThis.mDeviceMonitor != null)) {
/*  230 */       sThis.mDeviceMonitor.stop();
/*  231 */       sThis.mDeviceMonitor = null;
     }
 
/*  234 */     MonitorThread monitorThread = MonitorThread.getInstance();
/*  235 */     if (monitorThread != null) {
/*  236 */       monitorThread.quit();
     }
 
/*  239 */     sInitialized = false;
   }
 
   static boolean getClientSupport()
   {
/*  247 */     return sClientSupport;
   }
 
   public static InetSocketAddress getSocketAddress()
   {
/*  254 */     return sSocketAddr;
   }
 
   public static AndroidDebugBridge createBridge()
   {
/*  266 */     synchronized (sLock) {
/*  267 */       if (sThis != null) {
/*  268 */         return sThis;
       }
       try
       {
/*  272 */         sThis = new AndroidDebugBridge();
/*  273 */         sThis.start();
       } catch (InvalidParameterException e) {
/*  275 */         sThis = null;
       }
 
/*  282 */       IDebugBridgeChangeListener[] listenersCopy = (IDebugBridgeChangeListener[])sBridgeListeners.toArray(new IDebugBridgeChangeListener[sBridgeListeners.size()]);
 
/*  286 */       for (IDebugBridgeChangeListener listener : listenersCopy)
       {
         try
         {
/*  290 */           listener.bridgeChanged(sThis);
         } catch (Exception e) {
/*  292 */           Log.e("ddms", e);
         }
       }
 
/*  296 */       return sThis;
     }
   }
 
   public static AndroidDebugBridge createBridge(String osLocation, boolean forceNewBridge)
   {
       Log.e(ADB, "createBridge enter");
/*  314 */     synchronized (sLock) {
/*  315 */       if (sThis != null) {
/*  316 */         if ((sThis.mAdbOsLocation != null) && (sThis.mAdbOsLocation.equals(osLocation)) && (!forceNewBridge))
         {
                     Log.e(ADB, "return instance for adb");
/*  318 */           return sThis;
         }
 
/*  321 */         sThis.stop();
       }
 
       try
       {
           Log.e(ADB, "new object AndroidDebugBridge for adb");
/*  326 */         sThis = new AndroidDebugBridge(osLocation);
/*  327 */         if (!sThis.start())
/*  328 */           return null;
       }
       catch (InvalidParameterException e) {
/*  331 */         sThis = null;
       }
 
/*  338 */       IDebugBridgeChangeListener[] listenersCopy = (IDebugBridgeChangeListener[])sBridgeListeners.toArray(new IDebugBridgeChangeListener[sBridgeListeners.size()]);
       Log.e(ADB, "create " + listenersCopy.length + "listeners");
/*  342 */       for (IDebugBridgeChangeListener listener : listenersCopy)
       {
         try
         {
/*  346 */           listener.bridgeChanged(sThis);
         } catch (Exception e) {
/*  348 */           Log.e("ddms", e);
         }
       }
 
/*  352 */       return sThis;
     }
   }
 
   public static AndroidDebugBridge getBridge()
   {
/*  360 */     return sThis;
   }
 
   public static void disconnectBridge()
   {
/*  370 */     synchronized (sLock) {
/*  371 */       if (sThis != null) {
/*  372 */         sThis.stop();
/*  373 */         sThis = null;
 
/*  379 */         IDebugBridgeChangeListener[] listenersCopy = (IDebugBridgeChangeListener[])sBridgeListeners.toArray(new IDebugBridgeChangeListener[sBridgeListeners.size()]);
 
/*  383 */         for (IDebugBridgeChangeListener listener : listenersCopy)
         {
           try
           {
/*  387 */             listener.bridgeChanged(sThis);
           } catch (Exception e) {
/*  389 */             Log.e("ddms", e);
           }
         }
       }
     }
   }
 
   public static void addDebugBridgeChangeListener(IDebugBridgeChangeListener listener)
   {
/*  403 */     synchronized (sLock) {
/*  404 */       if (!sBridgeListeners.contains(listener)) {
/*  405 */         sBridgeListeners.add(listener);
/*  406 */         if (sThis != null)
         {
           try
           {
/*  410 */             listener.bridgeChanged(sThis);
           } catch (Exception e) {
/*  412 */             Log.e("ddms", e);
           }
         }
       }
     }
   }
 
   public static void removeDebugBridgeChangeListener(IDebugBridgeChangeListener listener)
   {
/*  425 */     synchronized (sLock) {
/*  426 */       sBridgeListeners.remove(listener);
     }
   }
 
   public static void addDeviceChangeListener(IDeviceChangeListener listener)
   {
/*  437 */     synchronized (sLock) {
/*  438 */       if (!sDeviceListeners.contains(listener))
/*  439 */         sDeviceListeners.add(listener);
     }
   }
 
   public static void removeDeviceChangeListener(IDeviceChangeListener listener)
   {
/*  451 */     synchronized (sLock) {
/*  452 */       sDeviceListeners.remove(listener);
     }
   }
 
   public static void addClientChangeListener(IClientChangeListener listener)
   {
/*  463 */     synchronized (sLock) {
/*  464 */       if (!sClientListeners.contains(listener))
/*  465 */         sClientListeners.add(listener);
     }
   }
 
   public static void removeClientChangeListener(IClientChangeListener listener)
   {
/*  476 */     synchronized (sLock) {
/*  477 */       sClientListeners.remove(listener);
     }
   }
 
   public IDevice[] getDevices()
   {
/*  488 */     synchronized (sLock) {
/*  489 */       if (this.mDeviceMonitor != null) {
/*  490 */         return this.mDeviceMonitor.getDevices();
       }
     }
 
/*  494 */     return new IDevice[0];
   }
 
   public boolean hasInitialDeviceList()
   {
/*  507 */     if (this.mDeviceMonitor != null) {
/*  508 */       return this.mDeviceMonitor.hasInitialDeviceList();
     }
 
/*  511 */     return false;
   }
 
   public void setSelectedClient(Client selectedClient)
   {
/*  519 */     MonitorThread monitorThread = MonitorThread.getInstance();
/*  520 */     if (monitorThread != null)
/*  521 */       monitorThread.setSelectedClient(selectedClient);
   }
 
   public boolean isConnected()
   {
/*  529 */     MonitorThread monitorThread = MonitorThread.getInstance();
/*  530 */     if ((this.mDeviceMonitor != null) && (monitorThread != null)) {
/*  531 */       return (this.mDeviceMonitor.isMonitoring()) && (monitorThread.getState() != Thread.State.TERMINATED);
     }
/*  533 */     return false;
   }
 
   public int getConnectionAttemptCount()
   {
/*  541 */     if (this.mDeviceMonitor != null) {
/*  542 */       return this.mDeviceMonitor.getConnectionAttemptCount();
     }
/*  544 */     return -1;
   }
 
   public int getRestartAttemptCount()
   {
/*  552 */     if (this.mDeviceMonitor != null) {
/*  553 */       return this.mDeviceMonitor.getRestartAttemptCount();
     }
/*  555 */     return -1;
   }
 
   private AndroidDebugBridge(String osLocation)
     throws InvalidParameterException
   {
/*  564 */     if ((osLocation == null) || (osLocation.isEmpty())) {
/*  565 */       throw new InvalidParameterException();
     }
/*  567 */     this.mAdbOsLocation = osLocation;
     try
     {
/*  570 */       checkAdbVersion();
     } catch (IOException e) {
/*  572 */       throw new IllegalArgumentException(e);
     }
   }
 
   private AndroidDebugBridge()
   {
   }
 
   private void checkAdbVersion()
     throws IOException
   {
/*  587 */     this.mVersionCheck = false;
 
/*  589 */     if (this.mAdbOsLocation == null) {
/*  590 */       return;
/*  593 */     }
 File adb = new File(this.mAdbOsLocation);
/*  594 */     ListenableFuture future = getAdbVersion(adb);
     AdbVersion version;
     try { version = (AdbVersion)future.get(5L, TimeUnit.SECONDS);
     } catch (InterruptedException e) {
/*  599 */       return;
     } catch (TimeoutException e) {
/*  601 */       String msg = "Unable to obtain result of 'adb version'";
/*  602 */       Log.logAndDisplay(Log.LogLevel.ERROR, "adb", msg);
/*  603 */       return;
     } catch (ExecutionException e) {
/*  605 */       Log.logAndDisplay(Log.LogLevel.ERROR, "adb", e.getCause().getMessage());
/*  606 */       Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
/*  607 */       return;
     }
 
/*  610 */     if (version.compareTo(MIN_ADB_VERSION) > 0) {
/*  611 */       this.mVersionCheck = true;
     } else {
/*  613 */       String message = String.format("Required minimum version of adb: %1$s.Current version is %2$s", new Object[] { MIN_ADB_VERSION, version });
 
/*  616 */       Log.logAndDisplay(Log.LogLevel.ERROR, "adb", message);
     }
   }
 
   public static ListenableFuture<AdbVersion> getAdbVersion(final File adb) {
/*  621 */     final SettableFuture future = SettableFuture.create();
/*  622 */     new Thread(new Runnable()
     {
       public void run() {
/*  625 */         ProcessBuilder pb = new ProcessBuilder(new String[] { adb.getPath(), "version" });
/*  626 */         pb.redirectErrorStream(true);
 
/*  628 */         Process p = null;
         try {
/*  630 */           p = pb.start();
         } catch (IOException e) {
/*  632 */           future.setException(e);
/*  633 */           return;
         }
 
/*  636 */         StringBuilder sb = new StringBuilder();
/*  637 */         BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
         try
         {
           String line;
/*  640 */           while ((line = br.readLine()) != null) {
/*  641 */             AdbVersion version = AdbVersion.parseFrom(line);
/*  642 */             if (version != AdbVersion.UNKNOWN) { future.set(version);
               return;
             }
/*  646 */             sb.append(line);
/*  647 */             sb.append('\n');
           } } catch (IOException e) { future.setException(e);
           return;
         }
         finally {
           try {
/*  654 */             br.close();
           } catch (IOException e) {
/*  656 */             future.setException(e);
           }
         }
 
/*  660 */         future.setException(new RuntimeException(new StringBuilder().append("Unable to detect adb version, adb output: ").append(sb.toString()).toString()));
       }
     }
     , "Obtaining adb version").start();
 
/*  664 */     return future;
   }
 
   boolean start()
   {
/*  673 */     if ((this.mAdbOsLocation != null) && (sAdbServerPort != 0) && ((!this.mVersionCheck) || (!startAdb()))) {
/*  674 */       return false;
     }
     			Log.e(ADB, "start adb real op");
/*  677 */     this.mStarted = true;
 
/*  680 */     this.mDeviceMonitor = new DeviceMonitor(this);
/*  681 */     this.mDeviceMonitor.start();
 
/*  683 */     return true;
   }
 
   boolean stop()
   {
/*  692 */     if (!this.mStarted) {
/*  693 */       return false;
     }
 
/*  697 */     if (this.mDeviceMonitor != null) {
/*  698 */       this.mDeviceMonitor.stop();
/*  699 */       this.mDeviceMonitor = null;
     }
 
/*  702 */     if (!stopAdb()) {
/*  703 */       return false;
     }
 
/*  706 */     this.mStarted = false;
/*  707 */     return true;
   }
 
   public boolean restart()
   {
/*  715 */     if (this.mAdbOsLocation == null) {
/*  716 */       Log.e("adb", "Cannot restart adb when AndroidDebugBridge is created without the location of adb.");
 
/*  718 */       return false;
     }
 
/*  721 */     if (sAdbServerPort == 0) {
/*  722 */       Log.e("adb", "ADB server port for restarting AndroidDebugBridge is not set.");
/*  723 */       return false;
     }
 
/*  726 */     if (!this.mVersionCheck) {
/*  727 */       Log.logAndDisplay(Log.LogLevel.ERROR, "adb", "Attempting to restart adb, but version check failed!");
 
/*  729 */       return false;
     }
/*  731 */     synchronized (this) {
/*  732 */       stopAdb();
 
/*  734 */       boolean restart = startAdb();
 
/*  736 */       if ((restart) && (this.mDeviceMonitor == null)) {
/*  737 */         this.mDeviceMonitor = new DeviceMonitor(this);
/*  738 */         this.mDeviceMonitor.start();
       }
 
/*  741 */       return restart;
     }
   }
 
   void deviceConnected(IDevice device)
   {
/*  763 */     IDeviceChangeListener[] listenersCopy = null;
/*  764 */     synchronized (sLock) {
/*  765 */       listenersCopy = (IDeviceChangeListener[])sDeviceListeners.toArray(new IDeviceChangeListener[sDeviceListeners.size()]);
     }
 
/*  770 */     for (IDeviceChangeListener listener : listenersCopy)
     {
       try
       {
/*  774 */         listener.deviceConnected(device);
       } catch (Exception e) {
/*  776 */         Log.e("ddms", e);
       }
     }
   }
 
   void deviceDisconnected(IDevice device)
   {
/*  799 */     IDeviceChangeListener[] listenersCopy = null;
/*  800 */     synchronized (sLock) {
/*  801 */       listenersCopy = (IDeviceChangeListener[])sDeviceListeners.toArray(new IDeviceChangeListener[sDeviceListeners.size()]);
     }
 
/*  806 */     for (IDeviceChangeListener listener : listenersCopy)
     {
       try
       {
/*  810 */         listener.deviceDisconnected(device);
       } catch (Exception e) {
/*  812 */         Log.e("ddms", e);
       }
     }
   }
 
   void deviceChanged(IDevice device, int changeMask)
   {
/*  835 */     IDeviceChangeListener[] listenersCopy = null;
/*  836 */     synchronized (sLock) {
/*  837 */       listenersCopy = (IDeviceChangeListener[])sDeviceListeners.toArray(new IDeviceChangeListener[sDeviceListeners.size()]);
     }
 
/*  842 */     for (IDeviceChangeListener listener : listenersCopy)
     {
       try
       {
/*  846 */         listener.deviceChanged(device, changeMask);
       } catch (Exception e) {
/*  848 */         Log.e("ddms", e);
       }
     }
   }
 
   void clientChanged(Client client, int changeMask)
   {
/*  872 */     IClientChangeListener[] listenersCopy = null;
/*  873 */     synchronized (sLock) {
/*  874 */       listenersCopy = (IClientChangeListener[])sClientListeners.toArray(new IClientChangeListener[sClientListeners.size()]);
     }
 
/*  880 */     for (IClientChangeListener listener : listenersCopy)
     {
       try
       {
/*  884 */         listener.clientChanged(client, changeMask);
       } catch (Exception e) {
/*  886 */         Log.e("ddms", e);
       }
     }
   }
 
   DeviceMonitor getDeviceMonitor()
   {
/*  895 */     return this.mDeviceMonitor;
   }
 
   synchronized boolean startAdb()
   {
/*  903 */     if (this.mAdbOsLocation == null) {
/*  904 */       Log.e("adb", "Cannot start adb when AndroidDebugBridge is created without the location of adb.");
 
/*  906 */       return false;
     }
 
/*  909 */     if (sAdbServerPort == 0) {
/*  910 */       Log.e("adb", "ADB server port for starting AndroidDebugBridge is not set.");
/*  911 */       return false;
     }
 
/*  915 */     int status = -1;
 				Log.d(ADB, "Launcher adb by start-server");
/*  917 */     String[] command = getAdbLaunchCommand("start-server");
/*  918 */     String commandString = Joiner.on(',').join(command);
     try {
/*  920 */       Log.d("ddms", String.format("Launching '%1$s' to ensure ADB is running.", new Object[] { commandString }));
/*  921 */       ProcessBuilder processBuilder = new ProcessBuilder(command);
/*  922 */       if (DdmPreferences.getUseAdbHost()) {
/*  923 */         String adbHostValue = DdmPreferences.getAdbHostValue();
/*  924 */         if ((adbHostValue != null) && (!adbHostValue.isEmpty()))
         {
/*  926 */           Map env = processBuilder.environment();
/*  927 */           env.put("ADBHOST", adbHostValue);
         }
       }
/*  930 */       Process proc = processBuilder.start();
 
/*  932 */       ArrayList errorOutput = new ArrayList();
/*  933 */       ArrayList stdOutput = new ArrayList();
/*  934 */       status = grabProcessOutput(proc, errorOutput, stdOutput, false);
     } catch (IOException ioe) {
/*  936 */       Log.e("ddms", "Unable to run 'adb': " + ioe.getMessage());
     }
     catch (InterruptedException ie) {
/*  939 */       Log.e("ddms", "Unable to run 'adb': " + ie.getMessage());
     }
 
/*  943 */     if (status != 0) {
/*  944 */       Log.e("ddms", String.format("'%1$s' failed -- run manually if necessary", new Object[] { commandString }));
 
/*  946 */       return false;
     }
/*  948 */     Log.e("ddms", String.format("'%1$s' succeeded", new Object[] { commandString }));
/*  949 */     return true;
   }
 
   private String[] getAdbLaunchCommand(String option)
   {
/*  954 */     List command = new ArrayList(4);
/*  955 */     command.add(this.mAdbOsLocation);
/*  956 */     if (sAdbServerPort != 5037) {
/*  957 */       command.add("-P");
/*  958 */       command.add(Integer.toString(sAdbServerPort));
     }
/*  960 */     command.add(option);
/*  961 */     return (String[])command.toArray(new String[command.size()]);
   }
 
   private synchronized boolean stopAdb()
   {
/*  970 */     if (this.mAdbOsLocation == null) {
/*  971 */       Log.e("adb", "Cannot stop adb when AndroidDebugBridge is created without the location of adb.");
 
/*  973 */       return false;
     }
 
/*  976 */     if (sAdbServerPort == 0) {
/*  977 */       Log.e("adb", "ADB server port for restarting AndroidDebugBridge is not set");
/*  978 */       return false;
     }
 
/*  982 */     int status = -1;
 
/*  984 */     String[] command = getAdbLaunchCommand("kill-server");
     try {
/*  986 */       Process proc = Runtime.getRuntime().exec(command);
/*  987 */       status = proc.waitFor();
     }
     catch (IOException ioe)
     {
     }
     catch (InterruptedException ie)
     {
     }
 
/*  996 */     String commandString = Joiner.on(',').join(command);
/*  997 */     if (status != 0) {
/*  998 */       Log.w("ddms", String.format("'%1$s' failed -- run manually if necessary", new Object[] { commandString }));
/*  999 */       return false;
     }
/* 1001 */     Log.d("ddms", String.format("'%1$s' succeeded", new Object[] { commandString }));
/* 1002 */     return true;
   }
 
   private int grabProcessOutput(final Process process, final ArrayList<String> errorOutput, final ArrayList<String> stdOutput, boolean waitForReaders)
     throws InterruptedException
   {
/* 1019 */     assert (errorOutput != null);
/* 1020 */     assert (stdOutput != null);
 
/* 1023 */     Thread t1 = new Thread("")
     {
       public void run()
       {
/* 1027 */         InputStreamReader is = new InputStreamReader(process.getErrorStream());
/* 1028 */         BufferedReader errReader = new BufferedReader(is);
         try
         {
           while (true) {
/* 1032 */             String line = errReader.readLine();
/* 1033 */             if (line == null) break;
/* 1034 */             Log.e("adb", line);
/* 1035 */             errorOutput.add(line);
           }
         }
         catch (IOException e)
         {
         }
       }
     };
/* 1046 */     Thread t2 = new Thread("")
     {
       public void run() {
/* 1049 */         InputStreamReader is = new InputStreamReader(process.getInputStream());
/* 1050 */         BufferedReader outReader = new BufferedReader(is);
         try
         {
           while (true) {
/* 1054 */             String line = outReader.readLine();
/* 1055 */             if (line == null) break;
/* 1056 */             Log.d("adb", line);
/* 1057 */             stdOutput.add(line);
           }
         }
         catch (IOException e)
         {
         }
       }
     };
/* 1068 */     t1.start();
/* 1069 */     t2.start();
 
/* 1074 */     if (waitForReaders) {
       try {
/* 1076 */         t1.join();
       } catch (InterruptedException e) {
       }
       try {
/* 1080 */         t2.join();
       }
       catch (InterruptedException e)
       {
       }
     }
/* 1086 */     return process.waitFor();
   }
 
   private static Object getLock()
   {
/* 1096 */     return sLock;
   }
 
   private static void initAdbSocketAddr()
   {
     try
     {
/* 1104 */       sAdbServerPort = getAdbServerPort();
                Log.d(DDMS, "using adb server port " + sAdbServerPort);
/* 1105 */       sHostAddr = InetAddress.getByName("127.0.0.1");
/* 1106 */       sSocketAddr = new InetSocketAddress(sHostAddr, sAdbServerPort);
     }
     catch (UnknownHostException e)
     {
     }
   }
 
   private static int getAdbServerPort()
   {
/* 1125 */     Integer prop = Integer.getInteger("ANDROID_ADB_SERVER_PORT");
/* 1126 */     if (prop != null) {
       try {
/* 1128 */         return validateAdbServerPort(prop.toString());
       } catch (IllegalArgumentException e) {
/* 1130 */         String msg = String.format("Invalid value (%1$s) for ANDROID_ADB_SERVER_PORT system property.", new Object[] { prop });
 
/* 1133 */         Log.w("ddms", msg);
       }
     }
 
     try
     {
/* 1139 */       String env = System.getenv("ANDROID_ADB_SERVER_PORT");
/* 1140 */       if (env != null) {
/* 1141 */         return validateAdbServerPort(env);
       }
 
     }
     catch (SecurityException ex)
     {
/* 1153 */       Log.w("ddms", "No access to env variables allowed by current security manager. If you've set ANDROID_ADB_SERVER_PORT: it's being ignored.");
     }
     catch (IllegalArgumentException e)
     {
/* 1157 */       String msg = String.format("Invalid value (%1$s) for ANDROID_ADB_SERVER_PORT environment variable (%2$s).", new Object[] { prop, e.getMessage() });
 
/* 1160 */       Log.w("ddms", msg);
     }
 
/* 1164 */     return DEFAULT_ADB_PORT;
   }
 
   private static int validateAdbServerPort(String adbServerPort)
     throws IllegalArgumentException
   {
     try
     {
/* 1178 */       int port = Integer.decode(adbServerPort).intValue();
/* 1179 */       if ((port <= 0) || (port >= 65535)) {
/* 1180 */         throw new IllegalArgumentException("Should be > 0 and < 65535");
       }
/* 1182 */       return port; } catch (NumberFormatException e) {
     }
/* 1184 */     throw new IllegalArgumentException("Not a valid port number");
   }
 
   public static abstract interface IClientChangeListener
   {
     public abstract void clientChanged(Client paramClient, int paramInt);
   }
 
   public static abstract interface IDeviceChangeListener
   {
     public abstract void deviceConnected(IDevice paramIDevice);
 
     public abstract void deviceDisconnected(IDevice paramIDevice);
 
     public abstract void deviceChanged(IDevice paramIDevice, int paramInt);
   }
 
   public static abstract interface IDebugBridgeChangeListener
   {
     public abstract void bridgeChanged(AndroidDebugBridge paramAndroidDebugBridge);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.AndroidDebugBridge
 * JD-Core Version:    0.6.2
 */