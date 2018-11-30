 package com.android.ddmlib;
 
 import com.android.ddmlib.log.LogReceiver;
 import com.google.common.base.CharMatcher;
 import com.google.common.base.Function;
 import com.google.common.base.Joiner;
 import com.google.common.base.Splitter;
 import com.google.common.collect.ImmutableList;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Sets;
 import com.google.common.util.concurrent.Atomics;
 import java.io.BufferedInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.nio.channels.SocketChannel;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.ExecutionException;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 import java.util.concurrent.atomic.AtomicReference;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 final class Device
   implements IDevice
 {
   static final String RE_EMULATOR_SN = "emulator-(\\d+)";
   private final String mSerialNumber;
   private String mAvdName = null;
 
   private IDevice.DeviceState mState = null;
 
   private boolean mIsRoot = false;
 
   private final PropertyFetcher mPropFetcher = new PropertyFetcher(this);
   private final Map<String, String> mMountPoints = new HashMap();
 
   private final BatteryFetcher mBatteryFetcher = new BatteryFetcher(this);
 
   private final List<Client> mClients = new ArrayList();
 
   private final Map<Integer, String> mClientInfo = new ConcurrentHashMap();
   private DeviceMonitor mMonitor;
   private static final String LOG_TAG = "Device";
   private static final char SEPARATOR = '-';
   private static final String UNKNOWN_PACKAGE = "";
   private static final long GET_PROP_TIMEOUT_MS = 100L;
   private static final long INITIAL_GET_PROP_TIMEOUT_MS = 250L;
   private static final int QUERY_IS_ROOT_TIMEOUT_MS = 1000;
   private static final long INSTALL_TIMEOUT_MINUTES = 1;
   private SocketChannel mSocketChannel;
   private Integer mLastBatteryLevel = null;
   private long mLastBatteryCheckTime = 0L;
   private static final String SCREEN_RECORDER_DEVICE_PATH = "/system/bin/screenrecord";
   private static final long LS_TIMEOUT_SEC = 2L;
   private Boolean mHasScreenRecorder;
   private Set<String> mHardwareCharacteristics;
   private int mApiLevel;
   private String mName;
   private static final CharMatcher UNSAFE_PM_INSTALL_SESSION_SPLIT_NAME_CHARS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.anyOf("_-")).negate();
 
   public String getSerialNumber()
   {
     return this.mSerialNumber;
   }
 
   public String getAvdName()
   {
     return this.mAvdName;
   }
 
   void setAvdName(String avdName)
   {
     if (!isEmulator()) {
       throw new IllegalArgumentException("Cannot set the AVD name of the device is not an emulator");
     }
 
     this.mAvdName = avdName;
   }
 
   public String getName()
   {
     if (this.mName != null) {
       return this.mName;
     }
 
     if (isOnline())
     {
       this.mName = constructName();
       return this.mName;
     }
     return constructName();
   }
 
   private String constructName()
   {
     if (isEmulator()) {
       String avdName = getAvdName();
       if (avdName != null) {
         return String.format("%s [%s]", new Object[] { avdName, getSerialNumber() });
       }
       return getSerialNumber();
     }
 
     String manufacturer = null;
     String model = null;
     try
     {
       manufacturer = cleanupStringForDisplay(getProperty("ro.product.manufacturer"));
       model = cleanupStringForDisplay(getProperty("ro.product.model"));
     }
     catch (Exception e)
     {
     }
 
     StringBuilder sb = new StringBuilder(20);
 
     if (manufacturer != null) {
       sb.append(manufacturer);
       sb.append('-');
     }
 
     if (model != null) {
       sb.append(model);
       sb.append('-');
     }
 
     sb.append(getSerialNumber());
     return sb.toString();
   }
 
   private String cleanupStringForDisplay(String s)
   {
     if (s == null) {
       return null;
     }
 
     StringBuilder sb = new StringBuilder(s.length());
     for (int i = 0; i < s.length(); i++) {
       char c = s.charAt(i);
 
       if (Character.isLetterOrDigit(c))
         sb.append(Character.toLowerCase(c));
       else {
         sb.append('_');
       }
     }
 
     return sb.toString();
   }
 
   public IDevice.DeviceState getState()
   {
     return this.mState;
   }
 
   void setState(IDevice.DeviceState state)
   {
     this.mState = state;
   }
 
   public Map<String, String> getProperties()
   {
     return Collections.unmodifiableMap(this.mPropFetcher.getProperties());
   }
 
   public int getPropertyCount()
   {
     return this.mPropFetcher.getProperties().size();
   }
 
   public String getProperty(String name)
   {
     Map properties = this.mPropFetcher.getProperties();
     long timeout = properties.isEmpty() ? 250L : 100L;
 
     Future future = this.mPropFetcher.getProperty(name);
     try {
       return (String)future.get(timeout, TimeUnit.MILLISECONDS);
     }
     catch (InterruptedException e) {
     }
     catch (ExecutionException e) {
     }
     catch (java.util.concurrent.TimeoutException e) {
     }
     return null;
   }
 
   public boolean arePropertiesSet()
   {
     return this.mPropFetcher.arePropertiesSet();
   }
 
   public String getPropertyCacheOrSync(String name)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     Future future = this.mPropFetcher.getProperty(name);
     try {
       return (String)future.get();
     }
     catch (InterruptedException e) {
     }
     catch (ExecutionException e) {
     }
     return null;
   }
 
   public String getPropertySync(String name)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     Future future = this.mPropFetcher.getProperty(name);
     try {
       return (String)future.get();
     }
     catch (InterruptedException e) {
     }
     catch (ExecutionException e) {
     }
     return null;
   }
 
   public Future<String> getSystemProperty(String name)
   {
     return this.mPropFetcher.getProperty(name);
   }
 
   public boolean supportsFeature(IDevice.Feature feature)
   {
     switch (feature.ordinal()) {
     case 1:
       if (getApiLevel() < 19) {
         return false;
       }
       if (this.mHasScreenRecorder == null) {
         this.mHasScreenRecorder = Boolean.valueOf(hasBinary("/system/bin/screenrecord"));
       }
       return this.mHasScreenRecorder.booleanValue();
     case 2:
       return getApiLevel() >= 19;
     }
     return false;
   }
 
   public boolean supportsFeature(IDevice.HardwareFeature feature)
   {
     if (this.mHardwareCharacteristics == null) {
       try {
         String characteristics = getProperty("ro.build.characteristics");
         if (characteristics == null) {
           return false;
         }
 
         this.mHardwareCharacteristics = Sets.newHashSet(Splitter.on(',').split(characteristics));
       } catch (Exception e) {
         this.mHardwareCharacteristics = Collections.emptySet();
       }
     }
 
     return this.mHardwareCharacteristics.contains(feature.getCharacteristic());
   }
 
   public int getApiLevel()
   {
     if (this.mApiLevel > 0) {
       return this.mApiLevel;
     }
     try
     {
       String buildApi = getProperty("ro.build.version.sdk");
       this.mApiLevel = (buildApi == null ? -1 : Integer.parseInt(buildApi));
       return this.mApiLevel; } catch (Exception e) {
     }
     return -1;
   }
 
   private boolean hasBinary(String path)
   {
     CountDownLatch latch = new CountDownLatch(1);
     CollectingOutputReceiver receiver = new CollectingOutputReceiver(latch);
     try {
       executeShellCommand(new StringBuilder().append("ls ").append(path).toString(), receiver, 2L, TimeUnit.SECONDS);
     } catch (Exception e) {
       return false;
     }
     try
     {
       latch.await(2L, TimeUnit.SECONDS);
     } catch (InterruptedException e) {
       return false;
     }
 
     String value = receiver.getOutput().trim();
     return !value.endsWith("No such file or directory");
   }
 
   public String getMountPoint(String name)
   {
     String mount = (String)this.mMountPoints.get(name);
     if (mount == null)
       try {
         mount = queryMountPoint(name);
         this.mMountPoints.put(name, mount);
       } catch (TimeoutException ignored) {
       } catch (AdbCommandRejectedException ignored) {
       } catch (ShellCommandUnresponsiveException ignored) {
       }
       catch (IOException ignored) {
       }
     return mount;
   }
 
   private String queryMountPoint(String name)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     final AtomicReference ref = Atomics.newReference();
     executeShellCommand(new StringBuilder().append("echo $").append(name).toString(), new MultiLineReceiver()
     {
       public boolean isCancelled() {
         return false;
       }
 
       public void processNewLines(String[] lines)
       {
         for (String line : lines)
           if (!line.isEmpty())
           {
             ref.set(line);
           }
       }
     });
     return (String)ref.get();
   }
 
   public String toString()
   {
     return this.mSerialNumber;
   }
 
   public boolean isOnline()
   {
     return this.mState == IDevice.DeviceState.ONLINE;
   }
 
   public boolean isEmulator()
   {
     return this.mSerialNumber.matches("emulator-(\\d+)");
   }
 
   public boolean isOffline()
   {
     return this.mState == IDevice.DeviceState.OFFLINE;
   }
 
   public boolean isBootLoader()
   {
     return this.mState == IDevice.DeviceState.BOOTLOADER;
   }
 
   public SyncService getSyncService()
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     SyncService syncService = new SyncService(AndroidDebugBridge.getSocketAddress(), this);
     if (syncService.openSync()) {
       return syncService;
     }
 
     return null;
   }
 
   public FileListingService getFileListingService()
   {
     return new FileListingService(this);
   }
 
   public RawImage getScreenshot()
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     return getScreenshot(0L, TimeUnit.MILLISECONDS);
   }
 
   public RawImage getScreenshot(long timeout, TimeUnit unit)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     return AdbHelper.getFrameBuffer(AndroidDebugBridge.getSocketAddress(), this, timeout, unit);
   }
 
   public void startScreenRecorder(String remoteFilePath, ScreenRecorderOptions options, IShellOutputReceiver receiver)
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException
   {
     executeShellCommand(getScreenRecorderCommand(remoteFilePath, options), receiver, 0L, null);
   }
 
   static String getScreenRecorderCommand(String remoteFilePath, ScreenRecorderOptions options)
   {
     StringBuilder sb = new StringBuilder();
 
     sb.append("screenrecord");
     sb.append(' ');
 
     if ((options.width > 0) && (options.height > 0)) {
       sb.append("--size ");
       sb.append(options.width);
       sb.append('x');
       sb.append(options.height);
       sb.append(' ');
     }
 
     if (options.bitrateMbps > 0) {
       sb.append("--bit-rate ");
       sb.append(options.bitrateMbps * 1000000);
       sb.append(' ');
     }
 
     if (options.timeLimit > 0L) {
       sb.append("--time-limit ");
       long seconds = TimeUnit.SECONDS.convert(options.timeLimit, options.timeLimitUnits);
       if (seconds > 180L) {
         seconds = 180L;
       }
       sb.append(seconds);
       sb.append(' ');
     }
 
     sb.append(remoteFilePath);
 
     return sb.toString();
   }

	 public void executeConnectCommand(String command, IShellOutputReceiver receiver)
			 throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
	 {
		 AdbHelper.executeConnectCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, DdmPreferences.getTimeOut());
	 }

	 public void executeConnectCommand(String command, IShellOutputReceiver receiver, int maxTimeToOutputResponse)
			 throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
	 {
		 AdbHelper.executeConnectCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, maxTimeToOutputResponse);
	 }

	 public void executeConnectCommand(String command, IShellOutputReceiver receiver, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
			 throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
	 {
		 AdbHelper.executeConnectCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, maxTimeToOutputResponse, maxTimeUnits);
	 }

   public void executeTCPIPCommand(String command, IShellOutputReceiver receiver)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     AdbHelper.executeTCPIPCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, DdmPreferences.getTimeOut());
   }

   public void executeTCPIPCommand(String command, IShellOutputReceiver receiver, int maxTimeToOutputResponse)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     AdbHelper.executeTCPIPCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, maxTimeToOutputResponse);
   }

   public void executeTCPIPCommand(String command, IShellOutputReceiver receiver, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     AdbHelper.executeTCPIPCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, maxTimeToOutputResponse, maxTimeUnits);
   }


   public void executeShellCommand(String command, IShellOutputReceiver receiver)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     AdbHelper.executeRemoteCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, DdmPreferences.getTimeOut());
   }
 
   public void executeShellCommand(String command, IShellOutputReceiver receiver, int maxTimeToOutputResponse)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     AdbHelper.executeRemoteCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, maxTimeToOutputResponse);
   }
 
   public void executeShellCommand(String command, IShellOutputReceiver receiver, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     AdbHelper.executeRemoteCommand(AndroidDebugBridge.getSocketAddress(), command, this, receiver, maxTimeToOutputResponse, maxTimeUnits);
   }
 
   public void runEventLogService(LogReceiver receiver)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.runEventLogService(AndroidDebugBridge.getSocketAddress(), this, receiver);
   }
 
   public void runLogService(String logname, LogReceiver receiver)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.runLogService(AndroidDebugBridge.getSocketAddress(), this, logname, receiver);
   }
 
   public void createForward(int localPort, int remotePort)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.createForward(AndroidDebugBridge.getSocketAddress(), this, String.format("tcp:%d", new Object[] { Integer.valueOf(localPort) }), String.format("tcp:%d", new Object[] { Integer.valueOf(remotePort) }));
   }

   public void createConnection(String host)
           throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.createConnection(AndroidDebugBridge.getSocketAddress(), this, host);
   }
 
   public void createForward(int localPort, String remoteSocketName, IDevice.DeviceUnixSocketNamespace namespace)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.createForward(AndroidDebugBridge.getSocketAddress(), this, String.format("tcp:%d", new Object[] { Integer.valueOf(localPort) }), String.format("%s:%s", new Object[] { namespace.getType(), remoteSocketName }));
   }
 
   public void removeForward(int localPort, int remotePort)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.removeForward(AndroidDebugBridge.getSocketAddress(), this, String.format("tcp:%d", new Object[] { Integer.valueOf(localPort) }), String.format("tcp:%d", new Object[] { Integer.valueOf(remotePort) }));
   }
 
   public void removeForward(int localPort, String remoteSocketName, IDevice.DeviceUnixSocketNamespace namespace)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.removeForward(AndroidDebugBridge.getSocketAddress(), this, String.format("tcp:%d", new Object[] { Integer.valueOf(localPort) }), String.format("%s:%s", new Object[] { namespace.getType(), remoteSocketName }));
   }
 
   Device(DeviceMonitor monitor, String serialNumber, IDevice.DeviceState deviceState)
   {
     this.mMonitor = monitor;
     this.mSerialNumber = serialNumber;
     this.mState = deviceState;
   }
 
   DeviceMonitor getMonitor() {
     return this.mMonitor;
   }
 
   public boolean hasClients()
   {
     synchronized (this.mClients) {
       return !this.mClients.isEmpty();
     }
   }
 
   public Client[] getClients()
   {
     synchronized (this.mClients) {
       return (Client[])this.mClients.toArray(new Client[this.mClients.size()]);
     }
   }
 
   public Client getClient(String applicationName)
   {
     synchronized (this.mClients) {
       for (Client c : this.mClients) {
         if (applicationName.equals(c.getClientData().getClientDescription())) {
           return c;
         }
       }
     }
 
     return null;
   }
 
   void addClient(Client client) {
     synchronized (this.mClients) {
       this.mClients.add(client);
     }
 
     addClientInfo(client);
   }
 
   List<Client> getClientList() {
     return this.mClients;
   }
 
   void clearClientList() {
     synchronized (this.mClients) {
       this.mClients.clear();
     }
 
     clearClientInfo();
   }
 
   void removeClient(Client client, boolean notify)
   {
     this.mMonitor.addPortToAvailableList(client.getDebuggerListenPort());
     synchronized (this.mClients) {
       this.mClients.remove(client);
     }
     if (notify) {
       this.mMonitor.getServer().deviceChanged(this, 2);
     }
 
     removeClientInfo(client);
   }
 
   void setClientMonitoringSocket(SocketChannel socketChannel)
   {
     this.mSocketChannel = socketChannel;
   }
 
   SocketChannel getClientMonitoringSocket()
   {
     return this.mSocketChannel;
   }
 
   void update(int changeMask) {
     this.mMonitor.getServer().deviceChanged(this, changeMask);
   }
 
   void update(Client client, int changeMask) {
     this.mMonitor.getServer().clientChanged(client, changeMask);
     updateClientInfo(client, changeMask);
   }
 
   void setMountingPoint(String name, String value) {
     this.mMountPoints.put(name, value);
   }
 
   private void addClientInfo(Client client) {
     ClientData cd = client.getClientData();
     setClientInfo(cd.getPid(), cd.getClientDescription());
   }
 
   private void updateClientInfo(Client client, int changeMask) {
     if ((changeMask & 0x1) == 1)
       addClientInfo(client);
   }
 
   private void removeClientInfo(Client client)
   {
     int pid = client.getClientData().getPid();
     this.mClientInfo.remove(Integer.valueOf(pid));
   }
 
   private void clearClientInfo() {
     this.mClientInfo.clear();
   }
 
   private void setClientInfo(int pid, String pkgName) {
     if (pkgName == null) {
       pkgName = "";
     }
 
     this.mClientInfo.put(Integer.valueOf(pid), pkgName);
   }
 
   public String getClientName(int pid)
   {
     String pkgName = (String)this.mClientInfo.get(Integer.valueOf(pid));
     return pkgName == null ? "" : pkgName;
   }
 
   public void pushFile(String local, String remote)
     throws IOException, AdbCommandRejectedException, TimeoutException, SyncException
   {
     SyncService sync = null;
     try {
       String targetFileName = getFileName(local);
 
       Log.d(targetFileName, String.format("Uploading %1$s onto device '%2$s'", new Object[] { targetFileName, getSerialNumber() }));
 
       sync = getSyncService();
       if (sync != null) {
         String message = String.format("Uploading file onto device '%1$s'", new Object[] { getSerialNumber() });
 
         Log.d("Device", message);
         sync.pushFile(local, remote, SyncService.getNullProgressMonitor());
       } else {
         throw new IOException("Unable to open sync connection!");
       }
     } catch (TimeoutException e) {
       Log.e("Device", "Error during Sync: timeout.");
       throw e;
     }
     catch (SyncException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw e;
     }
     catch (IOException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw e;
     }
     finally {
       if (sync != null)
         sync.close();
     }
   }
 
   public void pullFile(String remote, String local)
     throws IOException, AdbCommandRejectedException, TimeoutException, SyncException
   {
     SyncService sync = null;
     try {
       String targetFileName = getFileName(remote);
 
       Log.d(targetFileName, String.format("Downloading %1$s from device '%2$s'", new Object[] { targetFileName, getSerialNumber() }));
 
       sync = getSyncService();
       if (sync != null) {
         String message = String.format("Downloading file from device '%1$s'", new Object[] { getSerialNumber() });
 
         Log.d("Device", message);
         sync.pullFile(remote, local, SyncService.getNullProgressMonitor());
       } else {
         throw new IOException("Unable to open sync connection!");
       }
     } catch (TimeoutException e) {
       Log.e("Device", "Error during Sync: timeout.");
       throw e;
     }
     catch (SyncException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw e;
     }
     catch (IOException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw e;
     }
     finally {
       if (sync != null)
         sync.close();
     }
   }
 
   public void installPackage(String packageFilePath, boolean reinstall, String[] extraArgs)
     throws InstallException
   {
     try
     {
       String remoteFilePath = syncPackageToDevice(packageFilePath);
       installRemotePackage(remoteFilePath, reinstall, extraArgs);
       removeRemotePackage(remoteFilePath);
     } catch (IOException e) {
       throw new InstallException(e);
     } catch (AdbCommandRejectedException e) {
       throw new InstallException(e);
     } catch (TimeoutException e) {
       throw new InstallException(e);
     } catch (SyncException e) {
       throw new InstallException(e);
     }
   }
 
   public void installPackages(List<String> apkFilePaths, int timeOutInMs, boolean reinstall, String[] extraArgs)
     throws InstallException
   {
     assert (!apkFilePaths.isEmpty());
 
     if (getApiLevel() < 21) {
       Log.w("Internal error : installPackages invoked with device < 21 for %s", Joiner.on(",").join(apkFilePaths));
 
       if (apkFilePaths.size() == 1) {
         installPackage((String)apkFilePaths.get(0), reinstall, extraArgs);
         return;
       }
       Log.e("Internal error : installPackages invoked with device < 21 for multiple APK : %s", Joiner.on(",").join(apkFilePaths));
 
       throw new InstallException(new StringBuilder().append("Internal error : installPackages invoked with device < 21 for multiple APK : ").append(Joiner.on(",").join(apkFilePaths)).toString());
     }
 
     String mainPackageFilePath = (String)apkFilePaths.get(0);
     Log.d(mainPackageFilePath, String.format("Uploading main %1$s and %2$s split APKs onto device '%3$s'", new Object[] { mainPackageFilePath, Joiner.on(',').join(apkFilePaths), getSerialNumber() }));
     try
     {
       List extraArgsList = extraArgs != null ? ImmutableList.copyOf(extraArgs) : ImmutableList.of();
 
       String sessionId = createMultiInstallSession(apkFilePaths, extraArgsList, reinstall);
       if (sessionId == null) {
         Log.d(mainPackageFilePath, "Failed to establish session, quit installation");
         throw new InstallException("Failed to establish session");
       }
       Log.d(mainPackageFilePath, String.format("Established session id=%1$s", new Object[] { sessionId }));
 
       int index = 0;
       boolean allUploadSucceeded = true;
       while ((allUploadSucceeded) && (index < apkFilePaths.size())) {
         allUploadSucceeded = uploadAPK(sessionId, (String)apkFilePaths.get(index), index++, timeOutInMs);
       }
 
       String command = allUploadSucceeded ? new StringBuilder().append("pm install-commit ").append(sessionId).toString() : new StringBuilder().append("pm install-abandon ").append(sessionId).toString();
 
       InstallReceiver receiver = new InstallReceiver();
       executeShellCommand(command, receiver, timeOutInMs, TimeUnit.MILLISECONDS);
       String errorMessage = receiver.getErrorMessage();
       if (errorMessage != null) {
         String message = String.format("Failed to finalize session : %1$s", new Object[] { errorMessage });
         Log.e(mainPackageFilePath, message);
         throw new InstallException(message);
       }
 
       if (!allUploadSucceeded)
         throw new InstallException("Unable to upload some APKs");
     }
     catch (TimeoutException e) {
       Log.e("Device", "Error during Sync: timeout.");
       throw new InstallException(e);
     }
     catch (IOException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw new InstallException(e);
     }
     catch (AdbCommandRejectedException e) {
       throw new InstallException(e);
     } catch (ShellCommandUnresponsiveException e) {
       Log.e("Device", String.format("Error during shell execution: %1$s", new Object[] { e.getMessage() }));
       throw new InstallException(e);
     }
   }
 
   private String createMultiInstallSession(List<String> apkFileNames, Collection<String> extraArgs, boolean reinstall)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     List<File> apkFiles = Lists.transform(apkFileNames, new Function<String, File>()
     {
       	public File apply(String input) {
         		return new File(input);
       	}
     });
     long totalFileSize = 0L;
     for (File apkFile : apkFiles) {
       if ((apkFile.exists()) && (apkFile.isFile()))
         totalFileSize += apkFile.length();
       else {
         throw new IllegalArgumentException(new StringBuilder().append(apkFile.getAbsolutePath()).append(" is not a file").toString());
       }
     }
     StringBuilder parameters = new StringBuilder();
     if (reinstall) {
       parameters.append("-r ");
     }
     parameters.append(Joiner.on(' ').join(extraArgs));
     MultiInstallReceiver receiver = new MultiInstallReceiver();
     String cmd = String.format("pm install-create %1$s -S %2$d", new Object[] { parameters.toString(), Long.valueOf(totalFileSize) });
 
     executeShellCommand(cmd, receiver, DdmPreferences.getTimeOut());
     return receiver.getSessionId();
   }
 
   private boolean uploadAPK(String sessionId, String apkFilePath, int uniqueId, int timeOutInMs)
   {
     Log.d(sessionId, String.format("Uploading APK %1$s ", new Object[] { apkFilePath }));
     File fileToUpload = new File(apkFilePath);
     if (!fileToUpload.exists()) {
       Log.e(sessionId, String.format("File not found: %1$s", new Object[] { apkFilePath }));
       return false;
     }
     if (fileToUpload.isDirectory()) {
       Log.e(sessionId, String.format("Directory upload not supported: %1$s", new Object[] { apkFilePath }));
       return false;
     }
     String baseName = fileToUpload.getName().lastIndexOf(46) != -1 ? fileToUpload.getName().substring(0, fileToUpload.getName().lastIndexOf(46)) : fileToUpload.getName();
 
     baseName = UNSAFE_PM_INSTALL_SESSION_SPLIT_NAME_CHARS.replaceFrom(baseName, '_');
 
     String command = String.format("pm install-write -S %d %s %d_%s -", new Object[] { Long.valueOf(fileToUpload.length()), sessionId, Integer.valueOf(uniqueId), baseName });
 
     Log.d(sessionId, String.format("Executing : %1$s", new Object[] { command }));
     InputStream inputStream = null;
     try {
       inputStream = new BufferedInputStream(new FileInputStream(fileToUpload));
       InstallReceiver receiver = new InstallReceiver();
       AdbHelper.executeRemoteCommand(AndroidDebugBridge.getSocketAddress(), AdbHelper.AdbService.EXEC, command, this, receiver, timeOutInMs, TimeUnit.MILLISECONDS, inputStream);
 
       if (receiver.getErrorMessage() != null) {
         Log.e(sessionId, String.format("Error while uploading %1$s : %2$s", new Object[] { fileToUpload.getName(), receiver.getErrorMessage() }));
       }
       else {
         Log.d(sessionId, String.format("Successfully uploaded %1$s", new Object[] { fileToUpload.getName() }));
       }
       return receiver.getErrorMessage() == null;
     }
     catch (Exception e)
     {
       boolean bool;
       Log.e(sessionId, e);
       return false;
     } finally {
       if (inputStream != null)
         try {
           inputStream.close();
         } catch (IOException e) {
           Log.e(sessionId, e);
         }
     }
   }
 
   public String syncPackageToDevice(String localFilePath)
     throws IOException, AdbCommandRejectedException, TimeoutException, SyncException
   {
     SyncService sync = null;
     try {
       String packageFileName = getFileName(localFilePath);
       String remoteFilePath = String.format("/data/local/tmp/%1$s", new Object[] { packageFileName });
 
       Log.d(packageFileName, String.format("Uploading %1$s onto device '%2$s'", new Object[] { packageFileName, getSerialNumber() }));
 
       sync = getSyncService();
       String message;
       if (sync != null) {
         message = String.format("Uploading file onto device '%1$s'", new Object[] { getSerialNumber() });
 
         Log.d("Device", message);
         sync.pushFile(localFilePath, remoteFilePath, SyncService.getNullProgressMonitor());
       } else {
         throw new IOException("Unable to open sync connection!");
       }
       return remoteFilePath;
     } catch (TimeoutException e) {
       Log.e("Device", "Error during Sync: timeout.");
       throw e;
     }
     catch (SyncException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw e;
     }
     catch (IOException e) {
       Log.e("Device", String.format("Error during Sync: %1$s", new Object[] { e.getMessage() }));
       throw e;
     }
     finally {
       if (sync != null)
         sync.close();
     }
   }
 
   private static String getFileName(String filePath)
   {
     return new File(filePath).getName();
   }
 
   public void installRemotePackage(String remoteFilePath, boolean reinstall, String[] extraArgs) throws InstallException
   {
     try
     {
       InstallReceiver receiver = new InstallReceiver();
       StringBuilder optionString = new StringBuilder();
       if (reinstall) {
         optionString.append("-r ");
       }
       if (extraArgs != null) {
         optionString.append(Joiner.on(' ').join(extraArgs));
       }
       String cmd = String.format("pm install %1$s \"%2$s\"", new Object[] { optionString.toString(), remoteFilePath });
 
       executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
       String error = receiver.getErrorMessage();
       if (error != null)
         throw new InstallException(error);
     }
     catch (TimeoutException e) {
       throw new InstallException(e);
     } catch (AdbCommandRejectedException e) {
       throw new InstallException(e);
     } catch (ShellCommandUnresponsiveException e) {
       throw new InstallException(e);
     } catch (IOException e) {
       throw new InstallException(e);
     }
   }
 
   public void removeRemotePackage(String remoteFilePath) throws InstallException
   {
     try {
       executeShellCommand(String.format("rm \"%1$s\"", new Object[] { remoteFilePath }), new NullOutputReceiver(), INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
     }
     catch (IOException e) {
       throw new InstallException(e);
     } catch (TimeoutException e) {
       throw new InstallException(e);
     } catch (AdbCommandRejectedException e) {
       throw new InstallException(e);
     } catch (ShellCommandUnresponsiveException e) {
       throw new InstallException(e);
     }
   }
 
   public String uninstallPackage(String packageName) throws InstallException
   {
     try {
       InstallReceiver receiver = new InstallReceiver();
       executeShellCommand(new StringBuilder().append("pm uninstall ").append(packageName).toString(), receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
 
       return receiver.getErrorMessage();
     } catch (TimeoutException e) {
       throw new InstallException(e);
     } catch (AdbCommandRejectedException e) {
       throw new InstallException(e);
     } catch (ShellCommandUnresponsiveException e) {
       throw new InstallException(e);
     } catch (IOException e) {
       throw new InstallException(e);
     }
   }
 
   public void reboot(String into)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     AdbHelper.reboot(into, AndroidDebugBridge.getSocketAddress(), this);
   }
 
   public boolean root() throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException
   {
     if (!this.mIsRoot) {
       AdbHelper.root(AndroidDebugBridge.getSocketAddress(), this);
     }
     return isRoot();
   }
 
   public boolean isRoot() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     if (this.mIsRoot) {
       return true;
     }
     CollectingOutputReceiver receiver = new CollectingOutputReceiver();
     executeShellCommand("echo $USER_ID", receiver, 1000L, TimeUnit.MILLISECONDS);
     String userID = receiver.getOutput().trim();
     this.mIsRoot = userID.equals("0");
     return this.mIsRoot;
   }
 
   public Integer getBatteryLevel()
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException
   {
     return getBatteryLevel(300000L);
   }
 
   public Integer getBatteryLevel(long freshnessMs)
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException
   {
     Future futureBattery = getBattery(freshnessMs, TimeUnit.MILLISECONDS);
     try {
       return (Integer)futureBattery.get();
     } catch (InterruptedException e) {
       return null; } catch (ExecutionException e) {
     }
     return null;
   }
 
   public Future<Integer> getBattery()
   {
     return getBattery(5L, TimeUnit.MINUTES);
   }
 
   public Future<Integer> getBattery(long freshnessTime, TimeUnit timeUnit)
   {
     return this.mBatteryFetcher.getBattery(freshnessTime, timeUnit);
   }
 
   public List<String> getAbis()
   {
     String abiList = getProperty("ro.product.cpu.abilist");
     if (abiList != null) {
       return Lists.newArrayList(abiList.split(","));
     }
     List abis = Lists.newArrayListWithExpectedSize(2);
     String abi = getProperty("ro.product.cpu.abi");
     if (abi != null) {
       abis.add(abi);
     }
 
     abi = getProperty("ro.product.cpu.abi2");
     if (abi != null) {
       abis.add(abi);
     }
 
     return abis;
   }
 
   public int getDensity()
   {
     String densityValue = getProperty("ro.sf.lcd_density");
     if (densityValue != null) {
       try {
         return Integer.parseInt(densityValue);
       } catch (NumberFormatException e) {
         return -1;
       }
     }
 
     return -1;
   }
 
   public String getLanguage()
   {
     return (String)getProperties().get("persist.sys.language");
   }
 
   public String getRegion()
   {
     return getProperty("persist.sys.country");
   }
 
   static
   {
     String installTimeout = System.getenv("ADB_INSTALL_TIMEOUT");
     long time = 4L;
     if (installTimeout != null)
       try {
         time = Long.parseLong(installTimeout);
       }
       catch (NumberFormatException e)
       {
       }
   }
 
   private static class MultiInstallReceiver extends MultiLineReceiver
   {
     private static final Pattern successPattern = Pattern.compile("Success: .*\\[(\\d*)\\]");
 
     String sessionId = null;
 
     public boolean isCancelled()
     {
       return false;
     }
 
     public void processNewLines(String[] lines)
     {
       for (String line : lines) {
         Matcher matcher = successPattern.matcher(line);
         if (matcher.matches())
           this.sessionId = matcher.group(1);
       }
     }
 
     public String getSessionId()
     {
       return this.sessionId;
     }
   }
 
   private static final class InstallReceiver extends MultiLineReceiver
   {
     private static final String SUCCESS_OUTPUT = "Success";
     private static final Pattern FAILURE_PATTERN = Pattern.compile("Failure\\s+\\[(.*)\\]");
 
     private String mErrorMessage = null;
 
     public void processNewLines(String[] lines)
     {
       for (String line : lines)
         if (!line.isEmpty())
           if (line.startsWith("Success")) {
             this.mErrorMessage = null;
           } else {
             Matcher m = FAILURE_PATTERN.matcher(line);
             if (m.matches())
               this.mErrorMessage = m.group(1);
             else
               this.mErrorMessage = ("Unknown failure (" + line + ")");
           }
     }
 
     public boolean isCancelled()
     {
       return false;
     }
 
     public String getErrorMessage() {
       return this.mErrorMessage;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.Device
 * JD-Core Version:    0.6.2
 */
