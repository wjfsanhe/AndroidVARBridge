 package com.android.ddmlib;
 
 import com.android.ddmlib.utils.DebuggerPorts;
 import com.android.ddmlib.utils.Pair;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Maps;
 import com.google.common.collect.Queues;
 import com.google.common.util.concurrent.Uninterruptibles;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.Socket;
 import java.net.UnknownHostException;
 import java.nio.ByteBuffer;
 import java.nio.channels.AsynchronousCloseException;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.nio.channels.SocketChannel;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.TimeUnit;
 
 final class DeviceMonitor
 {
   private static final String ADB_TRACK_DEVICES_COMMAND = "host:track-devices";
   private static final String ADB_TRACK_JDWP_COMMAND = "track-jdwp";
   private static final String TAG = "DeviceMonitor";

/*  68 */   private final byte[] mLengthBuffer2 = new byte[4];
 
/*  70 */   private volatile boolean mQuit = false;
   private final AndroidDebugBridge mServer;
   private DeviceListMonitorTask mDeviceListMonitorTask;
   private Selector mSelector;
/*  77 */   private final List<Device> mDevices = Lists.newCopyOnWriteArrayList();
/*  78 */   private final DebuggerPorts mDebuggerPorts = new DebuggerPorts(DdmPreferences.getDebugPortBase());
 
/*  80 */   private final Map<Client, Integer> mClientsToReopen = new HashMap();
/*  81 */   private final BlockingQueue<Pair<SocketChannel, Device>> mChannelsToRegister = Queues.newLinkedBlockingQueue();
 
   DeviceMonitor(AndroidDebugBridge server)
   {
/*  90 */     this.mServer = server;
   }
 
   void start()
   {
/*  97 */     this.mDeviceListMonitorTask = new DeviceListMonitorTask(this.mServer, new DeviceListUpdateListener());
/*  98 */     new Thread(this.mDeviceListMonitorTask, "Device List Monitor").start();
   }
 
   void stop()
   {
/* 105 */     this.mQuit = true;
 
/* 107 */     if (this.mDeviceListMonitorTask != null) {
/* 108 */       this.mDeviceListMonitorTask.stop();
     }
 
/* 112 */     if (this.mSelector != null)
/* 113 */       this.mSelector.wakeup();
   }
 
   boolean isMonitoring()
   {
/* 121 */     return (this.mDeviceListMonitorTask != null) && (this.mDeviceListMonitorTask.isMonitoring());
   }
 
   int getConnectionAttemptCount() {
/* 125 */     return this.mDeviceListMonitorTask == null ? 0 : this.mDeviceListMonitorTask.getConnectionAttemptCount();
   }
 
   int getRestartAttemptCount()
   {
/* 130 */     return this.mDeviceListMonitorTask == null ? 0 : this.mDeviceListMonitorTask.getRestartAttemptCount();
   }
 
   boolean hasInitialDeviceList() {
/* 134 */     return (this.mDeviceListMonitorTask != null) && (this.mDeviceListMonitorTask.hasInitialDeviceList());
   }
 
   Device[] getDevices()
   {
/* 145 */     return (Device[])this.mDevices.toArray(new Device[0]);
   }
 
   AndroidDebugBridge getServer()
   {
/* 150 */     return this.mServer;
   }
 
   void addClientToDropAndReopen(Client client, int port) {
/* 154 */     synchronized (this.mClientsToReopen) {
/* 155 */       Log.d("DeviceMonitor", "Adding " + client + " to list of client to reopen (" + port + ").");
 
/* 157 */       if (this.mClientsToReopen.get(client) == null) {
/* 158 */         this.mClientsToReopen.put(client, Integer.valueOf(port));
       }
     }
/* 161 */     this.mSelector.wakeup();
   }
 
   private static SocketChannel openAdbConnection()
   {
     try
     {
/* 171 */       SocketChannel adbChannel = SocketChannel.open(AndroidDebugBridge.getSocketAddress());
/* 172 */       adbChannel.socket().setTcpNoDelay(true);
                if (adbChannel == null) Log.d(TAG, "open socket channel failed");
/* 173 */       return adbChannel; } catch (IOException e) {
                Log.d("DeviceMonitor", "exception occured during open socket");
                e.printStackTrace();
     }
/* 175 */     return null;
   }
 
   private void updateDevices(List<Device> newList)
   {
/* 183 */     DeviceListComparisonResult result = DeviceListComparisonResult.compare(this.mDevices, newList);
/* 184 */     for (IDevice device : result.removed) {
/* 185 */       removeDevice((Device)device);
/* 186 */       this.mServer.deviceDisconnected(device);
     }
 
/* 189 */     List<Device> newlyOnline = Lists.newArrayListWithExpectedSize(this.mDevices.size());
 
/* 191 */     for (Map.Entry entry : result.updated.entrySet()) {
/* 192 */       Device device = (Device)entry.getKey();
/* 193 */       device.setState((IDevice.DeviceState)entry.getValue());
/* 194 */       device.update(1);
 
/* 196 */       if (device.isOnline()) {
/* 197 */         newlyOnline.add(device);
       }
     }
 
/* 201 */     for (IDevice device : result.added) {
/* 202 */       this.mDevices.add((Device)device);
/* 203 */       this.mServer.deviceConnected(device);
/* 204 */       if (device.isOnline()) {
/* 205 */         newlyOnline.add((Device)device);
       }
     }
 
/* 209 */     if (AndroidDebugBridge.getClientSupport()) {
/* 210 */       for (Device device : newlyOnline) {
/* 211 */         if (!startMonitoringDevice(device)) {
/* 212 */           Log.e("DeviceMonitor", "Failed to start monitoring " + device.getSerialNumber());
         }
       }
 
     }
 
/* 218 */     for (Device device : newlyOnline) {
/* 219 */       queryAvdName(device);
 
/* 223 */       device.getSystemProperty("ro.build.version.sdk");
     }
   }
 
   private void removeDevice(Device device) {
/* 228 */     device.setState(IDevice.DeviceState.DISCONNECTED);
/* 229 */     device.clearClientList();
/* 230 */     this.mDevices.remove(device);
 
/* 232 */     SocketChannel channel = device.getClientMonitoringSocket();
/* 233 */     if (channel != null)
       try {
/* 235 */         channel.close();
       }
       catch (IOException e)
       {
       }
   }
 
   private static void queryAvdName(Device device) {
/* 243 */     if (!device.isEmulator()) {
/* 244 */       return;
     }
 
/* 247 */     EmulatorConsole console = EmulatorConsole.getConsole(device);
/* 248 */     if (console != null) {
/* 249 */       device.setAvdName(console.getAvdName());
/* 250 */       console.close();
     }
   }
 
   private boolean startMonitoringDevice(Device device)
   {
/* 260 */     SocketChannel socketChannel = openAdbConnection();
 
/* 262 */     if (socketChannel != null) {
       try {
/* 264 */         boolean result = sendDeviceMonitoringRequest(socketChannel, device);
/* 265 */         if (result)
         {
/* 267 */           if (this.mSelector == null) {
/* 268 */             startDeviceMonitorThread();
           }
 
/* 271 */           device.setClientMonitoringSocket(socketChannel);
 
/* 273 */           socketChannel.configureBlocking(false);
           try
           {
/* 276 */             this.mChannelsToRegister.put(Pair.of(socketChannel, device));
           }
           catch (InterruptedException e) {
           }
/* 280 */           this.mSelector.wakeup();
 
/* 282 */           return true;
         }
       }
       catch (TimeoutException e) {
         try {
/* 287 */           socketChannel.close();
         }
         catch (IOException e1) {
         }
/* 291 */         Log.d("DeviceMonitor", "Connection Failure when starting to monitor device '" + device + "' : timeout");
       }
       catch (AdbCommandRejectedException e)
       {
         try
         {
/* 297 */           socketChannel.close();
         }
         catch (IOException e1) {
         }
/* 301 */         Log.d("DeviceMonitor", "Adb refused to start monitoring device '" + device + "' : " + e.getMessage());
       }
       catch (IOException e)
       {
         try
         {
/* 307 */           socketChannel.close();
         }
         catch (IOException e1) {
         }
/* 311 */         Log.d("DeviceMonitor", "Connection Failure when starting to monitor device '" + device + "' : " + e.getMessage());
       }
 
     }
 
/* 317 */     return false;
   }
 
   private void startDeviceMonitorThread() throws IOException {
/* 321 */     this.mSelector = Selector.open();
/* 322 */     new Thread("Device Client Monitor")
     {
       public void run() {
/* 325 */         DeviceMonitor.this.deviceClientMonitorLoop();
       }
     }
/* 322 */     .start();
   }
 
   private void deviceClientMonitorLoop()
   {
     do {
       try
       {
/* 333 */         int count = this.mSelector.select();
 
/* 335 */         if (this.mQuit) {
/* 336 */           return;
         }
 
/* 339 */         synchronized (this.mClientsToReopen) {
/* 340 */           if (!this.mClientsToReopen.isEmpty()) {
/* 341 */             Set<Client> clients = this.mClientsToReopen.keySet();
/* 342 */             MonitorThread monitorThread = MonitorThread.getInstance();
 
/* 344 */             for (Client client : clients) {
/* 345 */               Device device = client.getDeviceImpl();
/* 346 */               int pid = client.getClientData().getPid();
 
/* 348 */               monitorThread.dropClient(client, false);
 
/* 352 */               Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.SECONDS);
 
/* 354 */               int port = ((Integer)this.mClientsToReopen.get(client)).intValue();
 
/* 356 */               if (port == -1) {
/* 357 */                 port = getNextDebuggerPort();
               }
/* 359 */               Log.d("DeviceMonitor", "Reopening " + client);
/* 360 */               openClient(device, pid, port, monitorThread);
/* 361 */               device.update(2);
             }
 
/* 364 */             this.mClientsToReopen.clear();
           }
 
         }
 
/* 369 */         while (!this.mChannelsToRegister.isEmpty()) {
           try {
/* 371 */             Pair data = (Pair)this.mChannelsToRegister.take();
/* 372 */             ((SocketChannel)data.getFirst()).register(this.mSelector, 1, data.getSecond());
           }
           catch (InterruptedException e)
           {
           }
         }
 
/* 379 */         if (count != 0)
         {
/* 383 */           Set keys = this.mSelector.selectedKeys();
/* 384 */           Iterator iter = keys.iterator();
 
/* 386 */           while (iter.hasNext()) {
/* 387 */             SelectionKey key = (SelectionKey)iter.next();
/* 388 */             iter.remove();
 
/* 390 */             if ((key.isValid()) && (key.isReadable())) {
/* 391 */               Object attachment = key.attachment();
 
/* 393 */               if ((attachment instanceof Device)) {
/* 394 */                 Device device = (Device)attachment;
 
/* 396 */                 SocketChannel socket = device.getClientMonitoringSocket();
 
/* 398 */                 if (socket != null)
                   try {
/* 400 */                     int length = readLength(socket, this.mLengthBuffer2);
 
/* 402 */                     processIncomingJdwpData(device, socket, length);
                   } catch (IOException ioe) {
/* 404 */                     Log.d("DeviceMonitor", "Error reading jdwp list: " + ioe.getMessage());
 
/* 406 */                     socket.close();
 
/* 409 */                     if (this.mDevices.contains(device)) {
/* 410 */                       Log.d("DeviceMonitor", "Restarting monitoring service for " + device);
 
/* 412 */                       startMonitoringDevice(device);
                     }
                   }
               }
             }
           }
         }
       } catch (IOException e) {
/* 420 */         Log.e("DeviceMonitor", "Connection error while monitoring clients.");
       }
     }
/* 423 */     while (!this.mQuit);
   }
 
   private static boolean sendDeviceMonitoringRequest(SocketChannel socket, Device device)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     try
     {
/* 431 */       AdbHelper.setDevice(socket, device);
/* 432 */       AdbHelper.write(socket, AdbHelper.formAdbRequest("track-jdwp"));
/* 433 */       AdbHelper.AdbResponse resp = AdbHelper.readAdbResponse(socket, false);
 
/* 435 */       if (!resp.okay)
       {
/* 437 */         Log.e("DeviceMonitor", "adb refused request: " + resp.message);
       }
 
/* 440 */       return resp.okay;
     } catch (TimeoutException e) {
/* 442 */       Log.e("DeviceMonitor", "Sending jdwp tracking request timed out!");
/* 443 */       throw e;
     } catch (IOException e) {
/* 445 */       Log.e("DeviceMonitor", "Sending jdwp tracking request failed!");
/* 446 */       throw e;
     }
   }
 
   private void processIncomingJdwpData(Device device, SocketChannel monitorSocket, int length)
     throws IOException
   {
/* 459 */     if (length >= 0)
     {
/* 461 */       Set newPids = new HashSet();
 
/* 464 */       if (length > 0) {
/* 465 */         byte[] buffer = new byte[length];
/* 466 */         String result = read(monitorSocket, buffer);
 
/* 469 */         String[] pids = result == null ? new String[0] : result.split("\n");
 
/* 471 */         for (String pid : pids) {
           try {
/* 473 */             newPids.add(Integer.valueOf(pid));
           }
           catch (NumberFormatException nfe)
           {
           }
         }
       }
 
/* 481 */       MonitorThread monitorThread = MonitorThread.getInstance();
 
/* 483 */       List<Client> clients = device.getClientList();
/* 484 */       Map<Integer, Client> existingClients = new HashMap();
 
/* 486 */       synchronized (clients) {
/* 487 */         for (Client c : clients) {
/* 488 */           existingClients.put(Integer.valueOf(c.getClientData().getPid()), c);
         }
       }
 
/* 492 */       Set clientsToRemove = new HashSet();
/* 493 */       for (Integer pid : existingClients.keySet()) {
/* 494 */         if (!newPids.contains(pid)) {
/* 495 */           clientsToRemove.add(existingClients.get(pid));
         }
       }
 
/* 499 */       Set pidsToAdd = new HashSet(newPids);
/* 500 */       pidsToAdd.removeAll(existingClients.keySet());
 
/* 502 */       monitorThread.dropClients(clientsToRemove, false);
 
/* 505 */       for (Iterator i$ = pidsToAdd.iterator(); i$.hasNext(); ) { int newPid = ((Integer)i$.next()).intValue();
/* 506 */         openClient(device, newPid, getNextDebuggerPort(), monitorThread);
       }
 
/* 509 */       if ((!pidsToAdd.isEmpty()) || (!clientsToRemove.isEmpty()))
/* 510 */         this.mServer.deviceChanged(device, 2);
     }
   }
 
   private static void openClient(Device device, int pid, int port, MonitorThread monitorThread)
   {
     SocketChannel clientSocket;
     try
     {
/* 521 */       clientSocket = AdbHelper.createPassThroughConnection(AndroidDebugBridge.getSocketAddress(), device, pid);
 
/* 525 */       clientSocket.configureBlocking(false);
     } catch (UnknownHostException uhe) {
/* 527 */       Log.d("DeviceMonitor", "Unknown Jdwp pid: " + pid);
/* 528 */       return;
     } catch (TimeoutException e) {
/* 530 */       Log.w("DeviceMonitor", "Failed to connect to client '" + pid + "': timeout");
 
/* 532 */       return;
     } catch (AdbCommandRejectedException e) {
/* 534 */       Log.w("DeviceMonitor", "Adb rejected connection to client '" + pid + "': " + e.getMessage());
 
/* 536 */       return;
     }
     catch (IOException ioe) {
/* 539 */       Log.w("DeviceMonitor", "Failed to connect to client '" + pid + "': " + ioe.getMessage());
 
/* 541 */       return;
     }
 
/* 544 */     createClient(device, pid, clientSocket, port, monitorThread);
   }
 
   private static void createClient(Device device, int pid, SocketChannel socket, int debuggerPort, MonitorThread monitorThread)
   {
/* 556 */     Client client = new Client(device, socket, pid);
 
/* 558 */     if (client.sendHandshake()) {
       try {
/* 560 */         if (AndroidDebugBridge.getClientSupport())
/* 561 */           client.listenForDebugger(debuggerPort);
       }
       catch (IOException ioe) {
/* 564 */         client.getClientData().setDebuggerConnectionStatus(ClientData.DebuggerStatus.ERROR);
/* 565 */         Log.e("ddms", "Can't bind to local " + debuggerPort + " for debugger");
       }
 
/* 569 */       client.requestAllocationStatus();
     } else {
/* 571 */       Log.e("ddms", "Handshake with " + client + " failed!");
     }
 
/* 580 */     if (client.isValid()) {
/* 581 */       device.addClient(client);
/* 582 */       monitorThread.addClient(client);
     }
   }
 
   private int getNextDebuggerPort() {
/* 587 */     return this.mDebuggerPorts.next();
   }
 
   void addPortToAvailableList(int port) {
/* 591 */     this.mDebuggerPorts.free(port);
   }
 
   private static int readLength(SocketChannel socket, byte[] buffer)
     throws IOException
   {
/* 602 */     String msg = read(socket, buffer);
 
/* 604 */     if (msg != null) {
       try {
/* 606 */         return Integer.parseInt(msg, 16);
       }
       catch (NumberFormatException nfe)
       {
       }
     }
 
/* 613 */     throw new IOException("Unable to read length");
   }
 
   private static String read(SocketChannel socket, byte[] buffer)
     throws IOException
   {
/* 624 */     ByteBuffer buf = ByteBuffer.wrap(buffer, 0, buffer.length);
 
/* 626 */     while (buf.position() != buf.limit())
     {
/* 629 */       int count = socket.read(buf);
/* 630 */       if (count < 0) {
/* 631 */         throw new IOException("EOF");
       }
     }
     try
     {
/* 636 */       return new String(buffer, 0, buf.position(), "ISO-8859-1"); } catch (UnsupportedEncodingException e) {
     }
/* 638 */     return null;
   }
 
   static class DeviceListMonitorTask
     implements Runnable
   {
/* 717 */     private final byte[] mLengthBuffer = new byte[4];
     private final AndroidDebugBridge mBridge;
     private final UpdateListener mListener;
/* 722 */     private SocketChannel mAdbConnection = null;
/* 723 */     private boolean mMonitoring = false;
/* 724 */     private int mConnectionAttempt = 0;
/* 725 */     private int mRestartAttemptCount = 0;
/* 726 */     private boolean mInitialDeviceListDone = false;
     private volatile boolean mQuit;
 
     public DeviceListMonitorTask(AndroidDebugBridge bridge, UpdateListener listener)
     {
/* 737 */       this.mBridge = bridge;
/* 738 */       this.mListener = listener;
     }
 
     public void run()
     {
       do {
/* 744 */         if (this.mAdbConnection == null) {
/* 745 */           Log.d("DeviceMonitor", "Opening adb connection");
/* 746 */           this.mAdbConnection = DeviceMonitor.openAdbConnection();
/* 747 */           if (this.mAdbConnection == null) {
/* 748 */             this.mConnectionAttempt += 1;
/* 749 */             Log.e("DeviceMonitor", "Connection attempts: " + this.mConnectionAttempt);
/* 750 */             if (this.mConnectionAttempt > 10) {
/* 751 */               if (!this.mBridge.startAdb()) {
/* 752 */                 this.mRestartAttemptCount += 1;
/* 753 */                 Log.e("DeviceMonitor", "adb restart attempts: " + this.mRestartAttemptCount);
               }
               else {
/* 756 */                 Log.i("DeviceMonitor", "adb restarted");
/* 757 */                 this.mRestartAttemptCount = 0;
               }
             }
/* 760 */             Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.SECONDS);
           } else {
/* 762 */             Log.d("DeviceMonitor", "Connected to adb for device monitoring");
/* 763 */             this.mConnectionAttempt = 0;
           }
         }
         try
         {
/* 768 */           if ((this.mAdbConnection != null) && (!this.mMonitoring)) {
/* 769 */             this.mMonitoring = sendDeviceListMonitoringRequest();
           }
 
/* 772 */           if (this.mMonitoring) {
/* 773 */             int length = DeviceMonitor.readLength(this.mAdbConnection, this.mLengthBuffer);
 
/* 775 */             if (length >= 0)
             {
/* 777 */               processIncomingDeviceData(length);
 
/* 780 */               this.mInitialDeviceListDone = true;
             }
           }
         } catch (AsynchronousCloseException ace) {
         }
         catch (TimeoutException ioe) {
/* 786 */           handleExceptionInMonitorLoop(ioe);
         } catch (IOException ioe) {
/* 788 */           handleExceptionInMonitorLoop(ioe);
         }
       }
/* 790 */       while (!this.mQuit);
     }
 
     private boolean sendDeviceListMonitoringRequest() throws TimeoutException, IOException {
/* 794 */       byte[] request = AdbHelper.formAdbRequest("host:track-devices");
       try
       {
/* 797 */         AdbHelper.write(this.mAdbConnection, request);
/* 798 */         AdbHelper.AdbResponse resp = AdbHelper.readAdbResponse(this.mAdbConnection, false);
/* 799 */         if (!resp.okay)
         {
/* 801 */           Log.e("DeviceMonitor", "adb refused request: " + resp.message);
         }
 
/* 804 */         return resp.okay;
       } catch (IOException e) {
/* 806 */         Log.e("DeviceMonitor", "Sending Tracking request failed!");
/* 807 */         this.mAdbConnection.close();
/* 808 */         throw e;
       }
     }
 
     private void handleExceptionInMonitorLoop(Exception e) {
/* 813 */       if (!this.mQuit) {
/* 814 */         if ((e instanceof TimeoutException))
/* 815 */           Log.e("DeviceMonitor", "Adb connection Error: timeout");
         else {
/* 817 */           Log.e("DeviceMonitor", "Adb connection Error:" + e.getMessage());
         }
/* 819 */         this.mMonitoring = false;
/* 820 */         if (this.mAdbConnection != null) {
           try {
/* 822 */             this.mAdbConnection.close();
           }
           catch (IOException ioe) {
           }
/* 826 */           this.mAdbConnection = null;
 
/* 828 */           this.mListener.connectionError(e);
         }
       }
     }
 
     private void processIncomingDeviceData(int length)
       throws IOException
     {
       Map result;
/* 836 */       if (length <= 0) {
/* 837 */         result = Collections.emptyMap();
       } else {
/* 839 */         String response = DeviceMonitor.read(this.mAdbConnection, new byte[length]);
/* 840 */         result = parseDeviceListResponse(response);
       }
 
/* 843 */       this.mListener.deviceListUpdate(result);
     }
 
     static Map<String, IDevice.DeviceState> parseDeviceListResponse(String result)
     {
/* 848 */       Map deviceStateMap = Maps.newHashMap();
/* 849 */       String[] devices = result == null ? new String[0] : result.split("\n");
 
/* 851 */       for (String d : devices) {
/* 852 */         String[] param = d.split("\t");
/* 853 */         if (param.length == 2)
         {
/* 855 */           deviceStateMap.put(param[0], IDevice.DeviceState.getState(param[1]));
         }
       }
/* 858 */       return deviceStateMap;
     }
 
     boolean isMonitoring() {
/* 862 */       return this.mMonitoring;
     }
 
     boolean hasInitialDeviceList() {
/* 866 */       return this.mInitialDeviceListDone;
     }
 
     int getConnectionAttemptCount() {
/* 870 */       return this.mConnectionAttempt;
     }
 
     int getRestartAttemptCount() {
/* 874 */       return this.mRestartAttemptCount;
     }
 
     public void stop() {
/* 878 */       this.mQuit = true;
 
/* 881 */       if (this.mAdbConnection != null)
         try {
/* 883 */           this.mAdbConnection.close();
         }
         catch (IOException ignored)
         {
         }
     }
 
     private static abstract interface UpdateListener
     {
       public abstract void connectionError(Exception paramException);
 
       public abstract void deviceListUpdate(Map<String, IDevice.DeviceState> paramMap);
     }
   }
 
   static class DeviceListComparisonResult
   {
     public final Map<IDevice, IDevice.DeviceState> updated;
     public final List<IDevice> added;
     public final List<IDevice> removed;
 
     private DeviceListComparisonResult(Map<IDevice, IDevice.DeviceState> updated, List<IDevice> added, List<IDevice> removed)
     {
/* 671 */       this.updated = updated;
/* 672 */       this.added = added;
/* 673 */       this.removed = removed;
     }
 
     public static DeviceListComparisonResult compare(List<? extends IDevice> previous, List<? extends IDevice> current)
     {
/* 679 */       current = Lists.newArrayList(current);
 
/* 681 */       Map updated = Maps.newHashMapWithExpectedSize(current.size());
/* 682 */       List added = Lists.newArrayListWithExpectedSize(1);
/* 683 */       List removed = Lists.newArrayListWithExpectedSize(1);
 
/* 685 */       for (IDevice device : previous) {
/* 686 */         IDevice currentDevice = find(current, device);
/* 687 */         if (currentDevice != null) {
/* 688 */           if (currentDevice.getState() != device.getState()) {
/* 689 */             updated.put(device, currentDevice.getState());
           }
/* 691 */           current.remove(currentDevice);
         } else {
/* 693 */           removed.add(device);
         }
       }
 
/* 697 */       added.addAll(current);
 
/* 699 */       return new DeviceListComparisonResult(updated, added, removed);
     }
 
     private static IDevice find(List<? extends IDevice> devices, IDevice device)
     {
/* 705 */       for (IDevice d : devices) {
/* 706 */         if (d.getSerialNumber().equals(device.getSerialNumber())) {
/* 707 */           return d;
         }
       }
 
/* 711 */       return null;
     }
   }
 
   private class DeviceListUpdateListener
     implements DeviceMonitor.DeviceListMonitorTask.UpdateListener
   {
     private DeviceListUpdateListener()
     {
     }
 
     public void connectionError(Exception e)
     {
/* 645 */       for (Device device : DeviceMonitor.this.mDevices) {
/* 646 */         DeviceMonitor.this.removeDevice(device);
/* 647 */         DeviceMonitor.this.mServer.deviceDisconnected(device);
       }
     }
 
     public void deviceListUpdate(Map<String, IDevice.DeviceState> devices)
     {
/* 653 */       List l = Lists.newArrayListWithExpectedSize(devices.size());
/* 654 */       for (Map.Entry entry : devices.entrySet()) {
/* 655 */         l.add(new Device(DeviceMonitor.this, (String)entry.getKey(), (IDevice.DeviceState)entry.getValue()));
       }
 
/* 658 */       DeviceMonitor.this.updateDevices(l);
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.DeviceMonitor
 * JD-Core Version:    0.6.2
 */