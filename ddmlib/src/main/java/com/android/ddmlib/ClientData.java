 package com.android.ddmlib;
 
 import java.io.PrintStream;
 import java.nio.BufferUnderflowException;
 import java.nio.ByteBuffer;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.TreeSet;
 
 public class ClientData
 {
   private static final String PRE_INITIALIZED = "<pre-initialized>";
   public static final String FEATURE_PROFILING = "method-trace-profiling";
   public static final String FEATURE_PROFILING_STREAMING = "method-trace-profiling-streaming";
   public static final String FEATURE_SAMPLING_PROFILER = "method-sample-profiling";
   public static final String FEATURE_OPENGL_TRACING = "opengl-tracing";
   public static final String FEATURE_VIEW_HIERARCHY = "view-hierarchy";
   public static final String FEATURE_HPROF = "hprof-heap-dump";
   public static final String FEATURE_HPROF_STREAMING = "hprof-heap-dump-streaming";
 
   @Deprecated
   private static IHprofDumpHandler sHprofDumpHandler;
   private static IMethodProfilingHandler sMethodProfilingHandler;
   private static IAllocationTrackingHandler sAllocationTrackingHandler;
   private boolean mIsDdmAware;
   private final int mPid;
   private String mVmIdentifier;
   private String mClientDescription;
   private int mUserId;
   private boolean mValidUserId;
   private String mAbi;
   private String mJvmFlags;
   private DebuggerStatus mDebuggerInterest;
/* 178 */   private final HashSet<String> mFeatures = new HashSet();
   private TreeMap<Integer, ThreadInfo> mThreadMap;
/* 184 */   private final HeapData mHeapData = new HeapData();
 
/* 186 */   private final HeapData mNativeHeapData = new HeapData();
 
/* 189 */   private HprofData mHprofData = null;
 
/* 191 */   private HashMap<Integer, HeapInfo> mHeapInfoMap = new HashMap();
 
/* 196 */   private ArrayList<NativeLibraryMapInfo> mNativeLibMapInfo = new ArrayList();
 
/* 200 */   private ArrayList<NativeAllocationInfo> mNativeAllocationList = new ArrayList();
   private int mNativeTotalMemory;
   private byte[] mAllocationsData;
   private AllocationInfo[] mAllocations;
/* 206 */   private AllocationTrackingStatus mAllocationStatus = AllocationTrackingStatus.UNKNOWN;
 
   @Deprecated
   private String mPendingHprofDump;
/* 211 */   private MethodProfilingStatus mProfilingStatus = MethodProfilingStatus.UNKNOWN;
   private String mPendingMethodProfiling;
 
   public void setHprofData(byte[] data)
   {
/* 432 */     this.mHprofData = new HprofData(data);
   }
 
   public void setHprofData(String filename) {
/* 436 */     this.mHprofData = new HprofData(filename);
   }
 
   public void clearHprofData() {
/* 440 */     this.mHprofData = null;
   }
 
   public HprofData getHprofData() {
/* 444 */     return this.mHprofData;
   }
 
   @Deprecated
   public static void setHprofDumpHandler(IHprofDumpHandler handler)
   {
/* 453 */     sHprofDumpHandler = handler;
   }
 
   @Deprecated
   static IHprofDumpHandler getHprofDumpHandler() {
/* 458 */     return sHprofDumpHandler;
   }
 
   public static void setMethodProfilingHandler(IMethodProfilingHandler handler)
   {
/* 466 */     sMethodProfilingHandler = handler;
   }
 
   static IMethodProfilingHandler getMethodProfilingHandler() {
/* 470 */     return sMethodProfilingHandler;
   }
 
   @Deprecated
   public static void setAllocationTrackingHandler(IAllocationTrackingHandler handler)
   {
/* 479 */     sAllocationTrackingHandler = handler;
   }
 
   @Deprecated
   static IAllocationTrackingHandler getAllocationTrackingHandler()
   {
/* 485 */     return sAllocationTrackingHandler;
   }
 
   ClientData(int pid)
   {
/* 492 */     this.mPid = pid;
 
/* 494 */     this.mDebuggerInterest = DebuggerStatus.DEFAULT;
/* 495 */     this.mThreadMap = new TreeMap();
   }
 
   public boolean isDdmAware()
   {
/* 502 */     return this.mIsDdmAware;
   }
 
   void isDdmAware(boolean aware)
   {
/* 509 */     this.mIsDdmAware = aware;
   }
 
   public int getPid()
   {
/* 516 */     return this.mPid;
   }
 
   public String getVmIdentifier()
   {
/* 523 */     return this.mVmIdentifier;
   }
 
   void setVmIdentifier(String ident)
   {
/* 530 */     this.mVmIdentifier = ident;
   }
 
   public String getClientDescription()
   {
/* 542 */     return this.mClientDescription;
   }
 
   public int getUserId()
   {
/* 550 */     return this.mUserId;
   }
 
   public boolean isValidUserId()
   {
/* 559 */     return this.mValidUserId;
   }
 
   public String getAbi()
   {
/* 565 */     return this.mAbi;
   }
 
   public String getJvmFlags()
   {
/* 570 */     return this.mJvmFlags;
   }
 
   void setClientDescription(String description)
   {
/* 581 */     if ((this.mClientDescription == null) && (!description.isEmpty()))
     {
/* 589 */       if (!"<pre-initialized>".equals(description))
/* 590 */         this.mClientDescription = description;
     }
   }
 
   void setUserId(int id)
   {
/* 596 */     this.mUserId = id;
/* 597 */     this.mValidUserId = true;
   }
 
   void setAbi(String abi) {
/* 601 */     this.mAbi = abi;
   }
 
   void setJvmFlags(String jvmFlags) {
/* 605 */     this.mJvmFlags = jvmFlags;
   }
 
   public DebuggerStatus getDebuggerConnectionStatus()
   {
/* 612 */     return this.mDebuggerInterest;
   }
 
   void setDebuggerConnectionStatus(DebuggerStatus status)
   {
/* 619 */     this.mDebuggerInterest = status;
   }
 
   synchronized void setHeapInfo(int heapId, long maxSizeInBytes, long sizeInBytes, long bytesAllocated, long objectsAllocated, long timeStamp, byte reason)
   {
/* 638 */     this.mHeapInfoMap.put(Integer.valueOf(heapId), new HeapInfo(maxSizeInBytes, sizeInBytes, bytesAllocated, objectsAllocated, timeStamp, reason));
   }
 
   public HeapData getVmHeapData()
   {
/* 646 */     return this.mHeapData;
   }
 
   HeapData getNativeHeapData()
   {
/* 653 */     return this.mNativeHeapData;
   }
 
   public synchronized Iterator<Integer> getVmHeapIds()
   {
/* 664 */     return this.mHeapInfoMap.keySet().iterator();
   }
 
   public synchronized HeapInfo getVmHeapInfo(int heapId)
   {
/* 675 */     return (HeapInfo)this.mHeapInfoMap.get(Integer.valueOf(heapId));
   }
 
   synchronized void addThread(int threadId, String threadName)
   {
/* 682 */     ThreadInfo attr = new ThreadInfo(threadId, threadName);
/* 683 */     this.mThreadMap.put(Integer.valueOf(threadId), attr);
   }
 
   synchronized void removeThread(int threadId)
   {
/* 690 */     this.mThreadMap.remove(Integer.valueOf(threadId));
   }
 
   public synchronized ThreadInfo[] getThreads()
   {
/* 699 */     Collection threads = this.mThreadMap.values();
/* 700 */     return (ThreadInfo[])threads.toArray(new ThreadInfo[threads.size()]);
   }
 
   synchronized ThreadInfo getThread(int threadId)
   {
/* 707 */     return (ThreadInfo)this.mThreadMap.get(Integer.valueOf(threadId));
   }
 
   synchronized void clearThreads() {
/* 711 */     this.mThreadMap.clear();
   }
 
   public synchronized List<NativeAllocationInfo> getNativeAllocationList()
   {
/* 719 */     return Collections.unmodifiableList(this.mNativeAllocationList);
   }
 
   synchronized void addNativeAllocation(NativeAllocationInfo allocInfo)
   {
/* 727 */     this.mNativeAllocationList.add(allocInfo);
   }
 
   synchronized void clearNativeAllocationInfo()
   {
/* 734 */     this.mNativeAllocationList.clear();
   }
 
   public synchronized int getTotalNativeMemory()
   {
/* 742 */     return this.mNativeTotalMemory;
   }
 
   synchronized void setTotalNativeMemory(int totalMemory) {
/* 746 */     this.mNativeTotalMemory = totalMemory;
   }
 
   synchronized void addNativeLibraryMapInfo(long startAddr, long endAddr, String library) {
/* 750 */     this.mNativeLibMapInfo.add(new NativeLibraryMapInfo(startAddr, endAddr, library));
   }
 
   public synchronized List<NativeLibraryMapInfo> getMappedNativeLibraries()
   {
/* 757 */     return Collections.unmodifiableList(this.mNativeLibMapInfo);
   }
 
   synchronized void setAllocationStatus(AllocationTrackingStatus status) {
/* 761 */     this.mAllocationStatus = status;
   }
 
   public synchronized AllocationTrackingStatus getAllocationStatus()
   {
/* 769 */     return this.mAllocationStatus;
   }
 
   synchronized void setAllocationsData(byte[] data) {
/* 773 */     this.mAllocationsData = data;
   }
 
   public synchronized byte[] getAllocationsData()
   {
/* 781 */     return this.mAllocationsData;
   }
 
   @Deprecated
   synchronized void setAllocations(AllocationInfo[] allocs) {
/* 786 */     this.mAllocations = allocs;
   }
 
   public synchronized AllocationInfo[] getAllocations()
   {
/* 795 */     if (this.mAllocationsData != null) {
/* 796 */       return AllocationsParser.parse(ByteBuffer.wrap(this.mAllocationsData));
     }
/* 798 */     return null;
   }
 
   void addFeature(String feature) {
/* 802 */     this.mFeatures.add(feature);
   }
 
   public boolean hasFeature(String feature)
   {
/* 814 */     return this.mFeatures.contains(feature);
   }
 
   @Deprecated
   void setPendingHprofDump(String pendingHprofDump)
   {
/* 823 */     this.mPendingHprofDump = pendingHprofDump;
   }
 
   @Deprecated
   String getPendingHprofDump()
   {
/* 831 */     return this.mPendingHprofDump;
   }
 
   @Deprecated
   public boolean hasPendingHprofDump() {
/* 836 */     return this.mPendingHprofDump != null;
   }
 
   synchronized void setMethodProfilingStatus(MethodProfilingStatus status) {
/* 840 */     this.mProfilingStatus = status;
   }
 
   public synchronized MethodProfilingStatus getMethodProfilingStatus()
   {
/* 848 */     return this.mProfilingStatus;
   }
 
   void setPendingMethodProfiling(String pendingMethodProfiling)
   {
/* 856 */     this.mPendingMethodProfiling = pendingMethodProfiling;
   }
 
   String getPendingMethodProfiling()
   {
/* 863 */     return this.mPendingMethodProfiling;
   }
 
   public static abstract interface IAllocationTrackingHandler
   {
     public abstract void onSuccess(byte[] paramArrayOfByte, Client paramClient);
   }
 
   public static abstract interface IMethodProfilingHandler
   {
     public abstract void onSuccess(String paramString, Client paramClient);
 
     public abstract void onSuccess(byte[] paramArrayOfByte, Client paramClient);
 
     public abstract void onStartFailure(Client paramClient, String paramString);
 
     public abstract void onEndFailure(Client paramClient, String paramString);
   }
 
   @Deprecated
   public static abstract interface IHprofDumpHandler
   {
     public abstract void onSuccess(String paramString, Client paramClient);
 
     public abstract void onSuccess(byte[] paramArrayOfByte, Client paramClient);
 
     public abstract void onEndFailure(Client paramClient, String paramString);
   }
 
   public static class HprofData
   {
     public final Type type;
     public final String filename;
     public final byte[] data;
 
     public HprofData(String filename)
     {
/* 346 */       this.type = Type.FILE;
/* 347 */       this.filename = filename;
/* 348 */       this.data = null;
     }
 
     public HprofData(byte[] data) {
/* 352 */       this.type = Type.DATA;
/* 353 */       this.data = data;
/* 354 */       this.filename = null;
     }
 
     public static enum Type
     {
/* 337 */       FILE, 
/* 338 */       DATA;
     }
   }
 
   public static class HeapInfo
   {
     public long maxSizeInBytes;
     public long sizeInBytes;
     public long bytesAllocated;
     public long objectsAllocated;
     public long timeStamp;
     public byte reason;
 
     public HeapInfo(long maxSizeInBytes, long sizeInBytes, long bytesAllocated, long objectsAllocated, long timeStamp, byte reason)
     {
/* 326 */       this.maxSizeInBytes = maxSizeInBytes;
/* 327 */       this.sizeInBytes = sizeInBytes;
/* 328 */       this.bytesAllocated = bytesAllocated;
/* 329 */       this.objectsAllocated = objectsAllocated;
/* 330 */       this.timeStamp = timeStamp;
/* 331 */       this.reason = reason;
     }
   }
 
   public static class HeapData
   {
/* 221 */     private TreeSet<HeapSegment> mHeapSegments = new TreeSet();
/* 222 */     private boolean mHeapDataComplete = false;
     private byte[] mProcessedHeapData;
     private Map<Integer, ArrayList<HeapSegment.HeapSegmentElement>> mProcessedHeapMap;
 
     public synchronized void clearHeapData()
     {
/* 233 */       this.mHeapSegments = new TreeSet();
/* 234 */       this.mHeapDataComplete = false;
     }
 
     synchronized void addHeapData(ByteBuffer data)
     {
/* 245 */       if (this.mHeapDataComplete)
/* 246 */         clearHeapData();
       HeapSegment hs;
       try
       {
/* 250 */         hs = new HeapSegment(data);
       } catch (BufferUnderflowException e) {
/* 252 */         System.err.println("Discarding short HPSG data (length " + data.limit() + ")");
/* 253 */         return;
       }
 
/* 256 */       this.mHeapSegments.add(hs);
     }
 
     synchronized void sealHeapData()
     {
/* 263 */       this.mHeapDataComplete = true;
     }
 
     public boolean isHeapDataComplete()
     {
/* 270 */       return this.mHeapDataComplete;
     }
 
     public Collection<HeapSegment> getHeapSegments()
     {
/* 279 */       if (isHeapDataComplete()) {
/* 280 */         return this.mHeapSegments;
       }
/* 282 */       return null;
     }
 
     public void setProcessedHeapData(byte[] heapData)
     {
/* 291 */       this.mProcessedHeapData = heapData;
     }
 
     public byte[] getProcessedHeapData()
     {
/* 300 */       return this.mProcessedHeapData;
     }
 
     public void setProcessedHeapMap(Map<Integer, ArrayList<HeapSegment.HeapSegmentElement>> heapMap) {
/* 304 */       this.mProcessedHeapMap = heapMap;
     }
 
     public Map<Integer, ArrayList<HeapSegment.HeapSegmentElement>> getProcessedHeapMap() {
/* 308 */       return this.mProcessedHeapMap;
     }
   }
 
   public static enum MethodProfilingStatus
   {
/*  94 */     UNKNOWN, 
 
/*  96 */     OFF, 
 
/*  98 */     TRACER_ON, 
 
/* 100 */     SAMPLER_ON;
   }
 
   public static enum AllocationTrackingStatus
   {
/*  79 */     UNKNOWN, 
 
/*  81 */     OFF, 
 
/*  83 */     ON;
   }
 
   public static enum DebuggerStatus
   {
/*  59 */     DEFAULT, 
 
/*  63 */     WAITING, 
 
/*  65 */     ATTACHED, 
 
/*  68 */     ERROR;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.ClientData
 * JD-Core Version:    0.6.2
 */