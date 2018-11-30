 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.BufferOverflowException;
 import java.nio.ByteBuffer;
 import java.nio.channels.Selector;
 import java.nio.channels.SocketChannel;
 import java.util.HashMap;
 import java.util.concurrent.TimeUnit;
 
 public class Client
 {
   private static final int SERVER_PROTOCOL_VERSION = 1;
   public static final int CHANGE_NAME = 1;
   public static final int CHANGE_DEBUGGER_STATUS = 2;
   public static final int CHANGE_PORT = 4;
   public static final int CHANGE_THREAD_MODE = 8;
   public static final int CHANGE_THREAD_DATA = 16;
   public static final int CHANGE_HEAP_MODE = 32;
   public static final int CHANGE_HEAP_DATA = 64;
   public static final int CHANGE_NATIVE_HEAP_DATA = 128;
   public static final int CHANGE_THREAD_STACKTRACE = 256;
   public static final int CHANGE_HEAP_ALLOCATIONS = 512;
   public static final int CHANGE_HEAP_ALLOCATION_STATUS = 1024;
   public static final int CHANGE_METHOD_PROFILING_STATUS = 2048;
   public static final int CHANGE_HPROF = 4096;
   public static final int CHANGE_INFO = 7;
   private SocketChannel mChan;
   private Debugger mDebugger;
   private int mDebuggerListenPort;
   private final HashMap<Integer, ChunkHandler> mOutstandingReqs;
   private ClientData mClientData;
   private boolean mThreadUpdateEnabled;
   private boolean mHeapInfoUpdateEnabled;
   private boolean mHeapSegmentUpdateEnabled;
   private static final int INITIAL_BUF_SIZE = 2048;
   private static final int MAX_BUF_SIZE = 838860800;
   private ByteBuffer mReadBuffer;
   private static final int WRITE_BUF_SIZE = 256;
   private ByteBuffer mWriteBuffer;
   private Device mDevice;
   private int mConnState;
   private static final int ST_INIT = 1;
   private static final int ST_NOT_JDWP = 2;
   private static final int ST_AWAIT_SHAKE = 10;
   private static final int ST_NEED_DDM_PKT = 11;
   private static final int ST_NOT_DDM = 12;
   private static final int ST_READY = 13;
   private static final int ST_ERROR = 20;
   private static final int ST_DISCONNECTED = 21;
 
   Client(Device device, SocketChannel chan, int pid)
   {
/* 133 */     this.mDevice = device;
/* 134 */     this.mChan = chan;
 
/* 136 */     this.mReadBuffer = ByteBuffer.allocate(2048);
/* 137 */     this.mWriteBuffer = ByteBuffer.allocate(256);
 
/* 139 */     this.mOutstandingReqs = new HashMap();
 
/* 141 */     this.mConnState = 1;
 
/* 143 */     this.mClientData = new ClientData(pid);
 
/* 145 */     this.mThreadUpdateEnabled = DdmPreferences.getInitialThreadUpdate();
/* 146 */     this.mHeapInfoUpdateEnabled = DdmPreferences.getInitialHeapUpdate();
/* 147 */     this.mHeapSegmentUpdateEnabled = DdmPreferences.getInitialHeapUpdate();
   }
 
   public String toString()
   {
/* 155 */     return "[Client pid: " + this.mClientData.getPid() + "]";
   }
 
   public IDevice getDevice()
   {
/* 162 */     return this.mDevice;
   }
 
   Device getDeviceImpl()
   {
/* 168 */     return this.mDevice;
   }
 
   public int getDebuggerListenPort()
   {
/* 175 */     return this.mDebuggerListenPort;
   }
 
   public boolean isDdmAware()
   {
/* 185 */     switch (this.mConnState) {
     case 1:
     case 2:
     case 10:
     case 11:
     case 12:
     case 20:
     case 21:
/* 193 */       return false;
     case 13:
/* 195 */       return true;
     case 3:
     case 4:
     case 5:
     case 6:
     case 7:
     case 8:
     case 9:
     case 14:
     case 15:
     case 16:
     case 17:
     case 18:
/* 197 */     case 19:  break;}
/* 198 */     return false;
   }
 
   public boolean isDebuggerAttached()
   {
/* 206 */     return this.mDebugger.isDebuggerAttached();
   }
 
   Debugger getDebugger()
   {
/* 213 */     return this.mDebugger;
   }
 
   public ClientData getClientData()
   {
/* 221 */     return this.mClientData;
   }
 
   public void executeGarbageCollector()
   {
     try
     {
/* 229 */       HandleHeap.sendHPGC(this);
     } catch (IOException ioe) {
/* 231 */       Log.w("ddms", "Send of HPGC message failed");
     }
   }
 
   public void dumpHprof()
   {
/* 240 */     boolean canStream = this.mClientData.hasFeature("hprof-heap-dump-streaming");
     try {
/* 242 */       if (canStream) {
/* 243 */         HandleHeap.sendHPDS(this);
       } else {
/* 245 */         String file = "/sdcard/" + this.mClientData.getClientDescription().replaceAll("\\:.*", "") + ".hprof";
 
/* 247 */         HandleHeap.sendHPDU(this, file);
       }
     } catch (IOException e) {
/* 250 */       Log.w("ddms", "Send of HPDU message failed");
     }
   }
 
   @Deprecated
   public void toggleMethodProfiling()
   {
     try
     {
/* 264 */       switch (this.mClientData.getMethodProfilingStatus().ordinal()) {
       case 1:
/* 266 */         stopMethodTracer();
/* 267 */         break;
       case 2:
/* 269 */         stopSamplingProfiler();
/* 270 */         break;
       case 3:
/* 272 */         startMethodTracer();
       }
     }
     catch (IOException e) {
/* 276 */       Log.w("ddms", "Toggle method profiling failed");
     }
   }
 
   private int getProfileBufferSize()
   {
/* 282 */     return DdmPreferences.getProfilerBufferSizeMb() * 1024 * 1024;
   }
 
   public void startMethodTracer() throws IOException {
/* 286 */     boolean canStream = this.mClientData.hasFeature("method-trace-profiling-streaming");
/* 287 */     int bufferSize = getProfileBufferSize();
/* 288 */     if (canStream) {
/* 289 */       HandleProfiling.sendMPSS(this, bufferSize, 0);
     } else {
/* 291 */       String file = "/sdcard/" + this.mClientData.getClientDescription().replaceAll("\\:.*", "") + ".trace";
 
/* 294 */       HandleProfiling.sendMPRS(this, file, bufferSize, 0);
     }
   }
 
   public void stopMethodTracer() throws IOException {
/* 299 */     boolean canStream = this.mClientData.hasFeature("method-trace-profiling-streaming");
 
/* 301 */     if (canStream)
/* 302 */       HandleProfiling.sendMPSE(this);
     else
/* 304 */       HandleProfiling.sendMPRE(this);
   }
 
   public void startSamplingProfiler(int samplingInterval, TimeUnit timeUnit) throws IOException
   {
/* 309 */     int bufferSize = getProfileBufferSize();
/* 310 */     HandleProfiling.sendSPSS(this, bufferSize, samplingInterval, timeUnit);
   }
 
   public void stopSamplingProfiler() throws IOException {
/* 314 */     HandleProfiling.sendSPSE(this);
   }
 
   public boolean startOpenGlTracing() {
/* 318 */     boolean canTraceOpenGl = this.mClientData.hasFeature("opengl-tracing");
/* 319 */     if (!canTraceOpenGl) {
/* 320 */       return false;
     }
     try
     {
/* 324 */       HandleViewDebug.sendStartGlTracing(this);
/* 325 */       return true;
     } catch (IOException e) {
/* 327 */       Log.w("ddms", "Start OpenGL Tracing failed");
/* 328 */     }return false;
   }
 
   public boolean stopOpenGlTracing()
   {
/* 333 */     boolean canTraceOpenGl = this.mClientData.hasFeature("opengl-tracing");
/* 334 */     if (!canTraceOpenGl) {
/* 335 */       return false;
     }
     try
     {
/* 339 */       HandleViewDebug.sendStopGlTracing(this);
/* 340 */       return true;
     } catch (IOException e) {
/* 342 */       Log.w("ddms", "Stop OpenGL Tracing failed");
/* 343 */     }return false;
   }
 
   public void requestMethodProfilingStatus()
   {
     try
     {
/* 357 */       HandleHeap.sendREAQ(this);
     } catch (IOException e) {
/* 359 */       Log.e("ddmlib", e);
     }
   }
 
   public void setThreadUpdateEnabled(boolean enabled)
   {
/* 371 */     this.mThreadUpdateEnabled = enabled;
/* 372 */     if (!enabled) {
/* 373 */       this.mClientData.clearThreads();
     }
     try
     {
/* 377 */       HandleThread.sendTHEN(this, enabled);
     }
     catch (IOException ioe) {
/* 380 */       ioe.printStackTrace();
     }
 
/* 383 */     update(8);
   }
 
   public boolean isThreadUpdateEnabled()
   {
/* 390 */     return this.mThreadUpdateEnabled;
   }
 
   public void requestThreadUpdate()
   {
/* 401 */     HandleThread.requestThreadUpdate(this);
   }
 
   public void requestThreadStackTrace(int threadId)
   {
/* 413 */     HandleThread.requestThreadStackCallRefresh(this, threadId);
   }
 
   public void setHeapUpdateEnabled(boolean enabled)
   {
/* 426 */     setHeapInfoUpdateEnabled(enabled);
/* 427 */     setHeapSegmentUpdateEnabled(enabled);
   }
 
   public void setHeapInfoUpdateEnabled(boolean enabled) {
/* 431 */     this.mHeapInfoUpdateEnabled = enabled;
     try
     {
/* 434 */       HandleHeap.sendHPIF(this, enabled ? 3 : 0);
     }
     catch (IOException ioe)
     {
     }
 
/* 441 */     update(32);
   }
 
   public void setHeapSegmentUpdateEnabled(boolean enabled) {
/* 445 */     this.mHeapSegmentUpdateEnabled = enabled;
     try
     {
/* 448 */       HandleHeap.sendHPSG(this, enabled ? 1 : 0, 0);
     }
     catch (IOException ioe)
     {
     }
 
/* 455 */     update(32);
   }
 
   void initializeHeapUpdateStatus() throws IOException {
/* 459 */     setHeapInfoUpdateEnabled(this.mHeapInfoUpdateEnabled);
   }
 
   public void updateHeapInfo()
   {
     try
     {
/* 467 */       HandleHeap.sendHPIF(this, 1);
     }
     catch (IOException ioe)
     {
     }
   }
 
   public boolean isHeapUpdateEnabled()
   {
/* 478 */     return (this.mHeapInfoUpdateEnabled) || (this.mHeapSegmentUpdateEnabled);
   }
 
   public boolean requestNativeHeapInformation()
   {
     try
     {
/* 490 */       HandleNativeHeap.sendNHGT(this);
/* 491 */       return true;
     } catch (IOException e) {
/* 493 */       Log.e("ddmlib", e);
     }
 
/* 496 */     return false;
   }
 
   public void enableAllocationTracker(boolean enable)
   {
     try
     {
/* 509 */       HandleHeap.sendREAE(this, enable);
     } catch (IOException e) {
/* 511 */       Log.e("ddmlib", e);
     }
   }
 
   public void requestAllocationStatus()
   {
     try
     {
/* 525 */       HandleHeap.sendREAQ(this);
     } catch (IOException e) {
/* 527 */       Log.e("ddmlib", e);
     }
   }
 
   public void requestAllocationDetails()
   {
     try
     {
/* 542 */       HandleHeap.sendREAL(this);
     } catch (IOException e) {
/* 544 */       Log.e("ddmlib", e);
     }
   }
 
   public void kill()
   {
     try
     {
/* 553 */       HandleExit.sendEXIT(this, 1);
     } catch (IOException ioe) {
/* 555 */       Log.w("ddms", "Send of EXIT message failed");
     }
   }
 
   void register(Selector sel)
     throws IOException
   {
/* 564 */     if (this.mChan != null)
/* 565 */       this.mChan.register(sel, 1, this);
   }
 
   public void setAsSelectedClient()
   {
/* 576 */     MonitorThread monitorThread = MonitorThread.getInstance();
/* 577 */     if (monitorThread != null)
/* 578 */       monitorThread.setSelectedClient(this);
   }
 
   public boolean isSelectedClient()
   {
/* 591 */     MonitorThread monitorThread = MonitorThread.getInstance();
/* 592 */     if (monitorThread != null) {
/* 593 */       return monitorThread.getSelectedClient() == this;
     }
 
/* 596 */     return false;
   }
 
   void listenForDebugger(int listenPort)
     throws IOException
   {
/* 604 */     this.mDebuggerListenPort = listenPort;
/* 605 */     this.mDebugger = new Debugger(this, listenPort);
   }
 
   boolean sendHandshake()
   {
/* 614 */     assert (this.mWriteBuffer.position() == 0);
     try
     {
/* 618 */       JdwpPacket.putHandshake(this.mWriteBuffer);
/* 619 */       int expectedLen = this.mWriteBuffer.position();
/* 620 */       this.mWriteBuffer.flip();
/* 621 */       if (this.mChan.write(this.mWriteBuffer) != expectedLen)
/* 622 */         throw new IOException("partial handshake write");
     }
     catch (IOException ioe) {
/* 625 */       Log.e("ddms-client", "IO error during handshake: " + ioe.getMessage());
/* 626 */       this.mConnState = 20;
/* 627 */       close(true);
/* 628 */       return false;
     }
     finally {
/* 631 */       this.mWriteBuffer.clear();
     }
 
/* 634 */     this.mConnState = 10;
 
/* 636 */     return true;
   }
 
   void sendAndConsume(JdwpPacket packet)
     throws IOException
   {
/* 646 */     sendAndConsume(packet, null);
   }
 
   void sendAndConsume(JdwpPacket packet, ChunkHandler replyHandler)
     throws IOException
   {
/* 664 */     SocketChannel chan = this.mChan;
/* 665 */     if (chan == null)
     {
/* 667 */       Log.v("ddms", "Not sending packet -- client is closed");
/* 668 */       return;
     }
 
/* 671 */     if (replyHandler != null)
     {
/* 677 */       addRequestId(packet.getId(), replyHandler);
     }
 
/* 684 */     synchronized (chan) {
       try {
/* 686 */         packet.writeAndConsume(chan);
       }
       catch (IOException ioe) {
/* 689 */         removeRequestId(packet.getId());
/* 690 */         throw ioe;
       }
     }
   }
 
   void forwardPacketToDebugger(JdwpPacket packet)
     throws IOException
   {
/* 703 */     Debugger dbg = this.mDebugger;
 
/* 705 */     if (dbg == null) {
/* 706 */       Log.d("ddms", "Discarding packet");
/* 707 */       packet.consume();
     } else {
/* 709 */       dbg.sendAndConsume(packet);
     }
   }
 
   void read()
     throws IOException, BufferOverflowException
   {
/* 725 */     if (this.mReadBuffer.position() == this.mReadBuffer.capacity()) {
/* 726 */       if (this.mReadBuffer.capacity() * 2 > 838860800) {
/* 727 */         Log.e("ddms", "Exceeded MAX_BUF_SIZE!");
/* 728 */         throw new BufferOverflowException();
       }
/* 730 */       Log.d("ddms", "Expanding read buffer to " + this.mReadBuffer.capacity() * 2);
 
/* 733 */       ByteBuffer newBuffer = ByteBuffer.allocate(this.mReadBuffer.capacity() * 2);
 
/* 736 */       this.mReadBuffer.position(0);
/* 737 */       newBuffer.put(this.mReadBuffer);
 
/* 739 */       this.mReadBuffer = newBuffer;
     }
 
/* 742 */     int count = this.mChan.read(this.mReadBuffer);
/* 743 */     if (count < 0) {
/* 744 */       throw new IOException("read failed");
     }
/* 746 */     Log.v("ddms", "Read " + count + " bytes from " + this);
   }
 
   JdwpPacket getJdwpPacket()
     throws IOException
   {
/* 767 */     if (this.mConnState == 10)
     {
/* 775 */       int result = JdwpPacket.findHandshake(this.mReadBuffer);
 
/* 777 */       switch (result) {
       case 1:
/* 779 */         Log.d("ddms", "Good handshake from client, sending HELO to " + this.mClientData.getPid());
 
/* 781 */         JdwpPacket.consumeHandshake(this.mReadBuffer);
/* 782 */         this.mConnState = 11;
/* 783 */         HandleHello.sendHelloCommands(this, 1);
 
/* 785 */         return getJdwpPacket();
       case 3:
/* 787 */         Log.d("ddms", "Bad handshake from client");
/* 788 */         if (MonitorThread.getInstance().getRetryOnBadHandshake())
         {
/* 791 */           this.mDevice.getMonitor().addClientToDropAndReopen(this, -1);
         }
         else
         {
/* 795 */           this.mConnState = 2;
/* 796 */           close(true);
         }
/* 798 */         break;
       case 2:
/* 800 */         Log.d("ddms", "No handshake from client yet.");
/* 801 */         break;
       default:
/* 803 */         Log.e("ddms", "Unknown packet while waiting for client handshake");
       }
/* 805 */       return null;
/* 806 */     }if ((this.mConnState == 11) || (this.mConnState == 12) || (this.mConnState == 13))
     {
/* 812 */       if (this.mReadBuffer.position() != 0) {
/* 813 */         Log.v("ddms", "Checking " + this.mReadBuffer.position() + " bytes");
       }
 
/* 816 */       return JdwpPacket.findPacket(this.mReadBuffer);
     }
 
/* 821 */     Log.e("ddms", "Receiving data in state = " + this.mConnState);
 
/* 824 */     return null;
   }
 
   private void addRequestId(int id, ChunkHandler handler)
   {
/* 832 */     synchronized (this.mOutstandingReqs) {
/* 833 */       Log.v("ddms", "Adding req 0x" + Integer.toHexString(id) + " to set");
 
/* 835 */       this.mOutstandingReqs.put(Integer.valueOf(id), handler);
     }
   }
 
   void removeRequestId(int id)
   {
/* 843 */     synchronized (this.mOutstandingReqs) {
/* 844 */       Log.v("ddms", "Removing req 0x" + Integer.toHexString(id) + " from set");
 
/* 846 */       this.mOutstandingReqs.remove(Integer.valueOf(id));
     }
   }
 
   ChunkHandler isResponseToUs(int id)
   {
/* 859 */     synchronized (this.mOutstandingReqs) {
/* 860 */       ChunkHandler handler = (ChunkHandler)this.mOutstandingReqs.get(Integer.valueOf(id));
/* 861 */       if (handler != null) {
/* 862 */         Log.v("ddms", "Found 0x" + Integer.toHexString(id) + " in request set - " + handler);
 
/* 865 */         return handler;
       }
     }
 
/* 869 */     return null;
   }
 
   void packetFailed(JdwpPacket reply)
   {
/* 877 */     if (this.mConnState == 11) {
/* 878 */       Log.d("ddms", "Marking " + this + " as non-DDM client");
/* 879 */       this.mConnState = 12;
/* 880 */     } else if (this.mConnState != 12) {
/* 881 */       Log.w("ddms", "WEIRD: got JDWP failure packet on DDM req");
     }
   }
 
   synchronized boolean ddmSeen()
   {
/* 896 */     if (this.mConnState == 11) {
/* 897 */       this.mConnState = 13;
/* 898 */       return false;
/* 899 */     }if (this.mConnState != 13) {
/* 900 */       Log.w("ddms", "WEIRD: in ddmSeen with state=" + this.mConnState);
     }
/* 902 */     return true;
   }
 
   void close(boolean notify)
   {
/* 916 */     Log.d("ddms", "Closing " + toString());
 
/* 918 */     this.mOutstandingReqs.clear();
     try
     {
/* 921 */       if (this.mChan != null) {
/* 922 */         this.mChan.close();
/* 923 */         this.mChan = null;
       }
 
/* 926 */       if (this.mDebugger != null) {
/* 927 */         this.mDebugger.close();
/* 928 */         this.mDebugger = null;
       }
     }
     catch (IOException ioe) {
/* 932 */       Log.w("ddms", "failed to close " + this);
     }
 
/* 936 */     this.mDevice.removeClient(this, notify);
   }
 
   public boolean isValid()
   {
/* 943 */     return this.mChan != null;
   }
 
   void update(int changeMask) {
/* 947 */     this.mDevice.update(this, changeMask);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.Client
 * JD-Core Version:    0.6.2
 */