 package com.android.ddmlib;
 
 import com.android.ddmlib.log.LogReceiver;
 import java.io.IOException;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Future;
 import java.util.concurrent.TimeUnit;
 
 public abstract interface IDevice extends IShellEnabledDevice
 {
   public static final String PROP_BUILD_VERSION = "ro.build.version.release";
   public static final String PROP_BUILD_API_LEVEL = "ro.build.version.sdk";
   public static final String PROP_BUILD_CODENAME = "ro.build.version.codename";
   public static final String PROP_BUILD_TAGS = "ro.build.tags";
   public static final String PROP_BUILD_TYPE = "ro.build.type";
   public static final String PROP_DEVICE_MODEL = "ro.product.model";
   public static final String PROP_DEVICE_MANUFACTURER = "ro.product.manufacturer";
   public static final String PROP_DEVICE_CPU_ABI_LIST = "ro.product.cpu.abilist";
   public static final String PROP_DEVICE_CPU_ABI = "ro.product.cpu.abi";
   public static final String PROP_DEVICE_CPU_ABI2 = "ro.product.cpu.abi2";
   public static final String PROP_BUILD_CHARACTERISTICS = "ro.build.characteristics";
   public static final String PROP_DEVICE_DENSITY = "ro.sf.lcd_density";
   public static final String PROP_DEVICE_LANGUAGE = "persist.sys.language";
   public static final String PROP_DEVICE_REGION = "persist.sys.country";
   public static final String PROP_DEBUGGABLE = "ro.debuggable";
   public static final String FIRST_EMULATOR_SN = "emulator-5554";
   public static final int CHANGE_STATE = 1;
   public static final int CHANGE_CLIENT_LIST = 2;
   public static final int CHANGE_BUILD_INFO = 4;
 
   @Deprecated
   public static final String PROP_BUILD_VERSION_NUMBER = "ro.build.version.sdk";
   public static final String MNT_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";
   public static final String MNT_ROOT = "ANDROID_ROOT";
   public static final String MNT_DATA = "ANDROID_DATA";
 
   public abstract String getSerialNumber();
 
   public abstract String getAvdName();
 
   public abstract DeviceState getState();
 
   @Deprecated
   public abstract Map<String, String> getProperties();
 
   @Deprecated
   public abstract int getPropertyCount();
 
   public abstract String getProperty(String paramString);
 
   public abstract boolean arePropertiesSet();
 
   @Deprecated
   public abstract String getPropertySync(String paramString)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
 
   @Deprecated
   public abstract String getPropertyCacheOrSync(String paramString)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
 
   public abstract boolean supportsFeature(Feature paramFeature);
 
   public abstract boolean supportsFeature(HardwareFeature paramHardwareFeature);
 
   public abstract String getMountPoint(String paramString);
 
   public abstract boolean isOnline();
 
   public abstract boolean isEmulator();
 
   public abstract boolean isOffline();
 
   public abstract boolean isBootLoader();
 
   public abstract boolean hasClients();
 
   public abstract Client[] getClients();
 
   public abstract Client getClient(String paramString);
 
   public abstract SyncService getSyncService()
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract FileListingService getFileListingService();
 
   public abstract RawImage getScreenshot()
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract RawImage getScreenshot(long paramLong, TimeUnit paramTimeUnit)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract void startScreenRecorder(String paramString, ScreenRecorderOptions paramScreenRecorderOptions, IShellOutputReceiver paramIShellOutputReceiver)
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException;
 
   @Deprecated
   public abstract void executeShellCommand(String paramString, IShellOutputReceiver paramIShellOutputReceiver, int paramInt)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
 
   public abstract void executeShellCommand(String paramString, IShellOutputReceiver paramIShellOutputReceiver)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;

     @Deprecated
     public abstract void executeTCPIPCommand(String paramString, IShellOutputReceiver paramIShellOutputReceiver, int paramInt)
             throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;

     public abstract void executeTCPIPCommand(String paramString, IShellOutputReceiver paramIShellOutputReceiver)
             throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
     public void executeTCPIPCommand(String command, IShellOutputReceiver receiver, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
             throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;

     public abstract void executeConnectCommand(String paramString, IShellOutputReceiver paramIShellOutputReceiver)
             throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;
     public void executeConnectCommand(String command, IShellOutputReceiver receiver, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
             throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException;

   public abstract void runEventLogService(LogReceiver paramLogReceiver)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract void runLogService(String paramString, LogReceiver paramLogReceiver)
     throws TimeoutException, AdbCommandRejectedException, IOException;

   public abstract void createConnection(String host)
             throws TimeoutException, AdbCommandRejectedException, IOException;
   public abstract void createForward(int paramInt1, int paramInt2)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract void createForward(int paramInt, String paramString, DeviceUnixSocketNamespace paramDeviceUnixSocketNamespace)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract void removeForward(int paramInt1, int paramInt2)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract void removeForward(int paramInt, String paramString, DeviceUnixSocketNamespace paramDeviceUnixSocketNamespace)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract String getClientName(int paramInt);
 
   public abstract void pushFile(String paramString1, String paramString2)
     throws IOException, AdbCommandRejectedException, TimeoutException, SyncException;
 
   public abstract void pullFile(String paramString1, String paramString2)
     throws IOException, AdbCommandRejectedException, TimeoutException, SyncException;
 
   public abstract void installPackage(String paramString, boolean paramBoolean, String[] paramArrayOfString)
     throws InstallException;
 
   public abstract void installPackages(List<String> paramList, int paramInt, boolean paramBoolean, String[] paramArrayOfString)
     throws InstallException;
 
   public abstract String syncPackageToDevice(String paramString)
     throws TimeoutException, AdbCommandRejectedException, IOException, SyncException;
 
   public abstract void installRemotePackage(String paramString, boolean paramBoolean, String[] paramArrayOfString)
     throws InstallException;
 
   public abstract void removeRemotePackage(String paramString)
     throws InstallException;
 
   public abstract String uninstallPackage(String paramString)
     throws InstallException;
 
   public abstract void reboot(String paramString)
     throws TimeoutException, AdbCommandRejectedException, IOException;
 
   public abstract boolean root()
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException;
 
   public abstract boolean isRoot()
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException;
 
   @Deprecated
   public abstract Integer getBatteryLevel()
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException;
 
   @Deprecated
   public abstract Integer getBatteryLevel(long paramLong)
     throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException;
 
   public abstract Future<Integer> getBattery();
 
   public abstract Future<Integer> getBattery(long paramLong, TimeUnit paramTimeUnit);
 
   public abstract List<String> getAbis();
 
   public abstract int getDensity();
 
   public abstract String getLanguage();
 
   public abstract String getRegion();
 
   public abstract int getApiLevel();
 
   public static enum DeviceUnixSocketNamespace
   {
/* 130 */     ABSTRACT("localabstract"), 
/* 131 */     FILESYSTEM("localfilesystem"), 
/* 132 */     RESERVED("localreserved");
 
     private String mType;
 
     private DeviceUnixSocketNamespace(String type) {
/* 137 */       this.mType = type;
     }
 
     String getType() {
/* 141 */       return this.mType;
     }
   }
 
   public static enum DeviceState
   {
/*  95 */     BOOTLOADER("bootloader"), 
/*  96 */     OFFLINE("offline"), 
/*  97 */     ONLINE("device"), 
/*  98 */     RECOVERY("recovery"), 
/*  99 */     UNAUTHORIZED("unauthorized"), 
/* 100 */     DISCONNECTED("disconnected");
 
     private String mState;
 
     private DeviceState(String state)
     {
/* 106 */       this.mState = state;
     }
 
     public static DeviceState getState(String state)
     {
/* 117 */       for (DeviceState deviceState : values()) {
/* 118 */         if (deviceState.mState.equals(state)) {
/* 119 */           return deviceState;
         }
       }
/* 122 */       return null;
     }
   }
 
   public static enum HardwareFeature
   {
/*  69 */     WATCH("watch"), 
/*  70 */     TV("tv");
 
     private final String mCharacteristic;
 
     private HardwareFeature(String characteristic) {
/*  75 */       this.mCharacteristic = characteristic;
     }
 
     public String getCharacteristic() {
/*  79 */       return this.mCharacteristic;
     }
   }
 
   public static enum Feature
   {
/*  63 */     SCREEN_RECORD, 
/*  64 */     PROCSTATS;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.IDevice
 * JD-Core Version:    0.6.2
 */