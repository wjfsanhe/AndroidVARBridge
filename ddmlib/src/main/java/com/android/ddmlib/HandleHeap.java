 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.BufferUnderflowException;
 import java.nio.ByteBuffer;
 
 final class HandleHeap extends ChunkHandler
 {
/*  31 */   public static final int CHUNK_HPIF = type("HPIF");
/*  32 */   public static final int CHUNK_HPST = type("HPST");
/*  33 */   public static final int CHUNK_HPEN = type("HPEN");
/*  34 */   public static final int CHUNK_HPSG = type("HPSG");
/*  35 */   public static final int CHUNK_HPGC = type("HPGC");
/*  36 */   public static final int CHUNK_HPDU = type("HPDU");
/*  37 */   public static final int CHUNK_HPDS = type("HPDS");
/*  38 */   public static final int CHUNK_REAE = type("REAE");
/*  39 */   public static final int CHUNK_REAQ = type("REAQ");
/*  40 */   public static final int CHUNK_REAL = type("REAL");
   public static final int WHEN_DISABLE = 0;
   public static final int WHEN_GC = 1;
   public static final int WHAT_MERGE = 0;
   public static final int WHAT_OBJ = 1;
   public static final int HPIF_WHEN_NEVER = 0;
   public static final int HPIF_WHEN_NOW = 1;
   public static final int HPIF_WHEN_NEXT_GC = 2;
   public static final int HPIF_WHEN_EVERY_GC = 3;
/*  54 */   private static final HandleHeap mInst = new HandleHeap();
 
   public static void register(MonitorThread mt)
   {
/*  62 */     mt.registerChunkHandler(CHUNK_HPIF, mInst);
/*  63 */     mt.registerChunkHandler(CHUNK_HPST, mInst);
/*  64 */     mt.registerChunkHandler(CHUNK_HPEN, mInst);
/*  65 */     mt.registerChunkHandler(CHUNK_HPSG, mInst);
/*  66 */     mt.registerChunkHandler(CHUNK_HPDS, mInst);
/*  67 */     mt.registerChunkHandler(CHUNK_REAQ, mInst);
/*  68 */     mt.registerChunkHandler(CHUNK_REAL, mInst);
   }
 
   public void clientReady(Client client)
     throws IOException
   {
/*  76 */     client.initializeHeapUpdateStatus();
   }
 
   public void clientDisconnected(Client client)
   {
   }
 
   public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
   {
/*  90 */     Log.d("ddm-heap", "handling " + ChunkHandler.name(type));
 
/*  92 */     if (type == CHUNK_HPIF)
/*  93 */       handleHPIF(client, data);
/*  94 */     else if (type == CHUNK_HPST)
/*  95 */       handleHPST(client, data);
/*  96 */     else if (type == CHUNK_HPEN)
/*  97 */       handleHPEN(client, data);
/*  98 */     else if (type == CHUNK_HPSG)
/*  99 */       handleHPSG(client, data);
/* 100 */     else if (type == CHUNK_HPDU)
/* 101 */       handleHPDU(client, data);
/* 102 */     else if (type == CHUNK_HPDS)
/* 103 */       handleHPDS(client, data);
/* 104 */     else if (type == CHUNK_REAQ)
/* 105 */       handleREAQ(client, data);
/* 106 */     else if (type == CHUNK_REAL)
/* 107 */       handleREAL(client, data);
     else
/* 109 */       handleUnknownChunk(client, type, data, isReply, msgId);
   }
 
   private void handleHPIF(Client client, ByteBuffer data)
   {
/* 117 */     Log.d("ddm-heap", "HPIF!");
     try {
/* 119 */       int numHeaps = data.getInt();
 
/* 121 */       for (int i = 0; i < numHeaps; i++) {
/* 122 */         int heapId = data.getInt();
/* 123 */         long timeStamp = data.getLong();
/* 124 */         byte reason = data.get();
/* 125 */         long maxHeapSize = data.getInt() & 0xFFFFFFFF;
/* 126 */         long heapSize = data.getInt() & 0xFFFFFFFF;
/* 127 */         long bytesAllocated = data.getInt() & 0xFFFFFFFF;
/* 128 */         long objectsAllocated = data.getInt() & 0xFFFFFFFF;
 
/* 130 */         client.getClientData().setHeapInfo(heapId, maxHeapSize, heapSize, bytesAllocated, objectsAllocated, timeStamp, reason);
 
/* 132 */         client.update(64);
       }
     } catch (BufferUnderflowException ex) {
/* 135 */       Log.w("ddm-heap", "malformed HPIF chunk from client");
     }
   }
 
   public static void sendHPIF(Client client, int when)
     throws IOException
   {
/* 143 */     ByteBuffer rawBuf = allocBuffer(1);
/* 144 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 145 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 147 */     buf.put((byte)when);
 
/* 149 */     finishChunkPacket(packet, CHUNK_HPIF, buf.position());
/* 150 */     Log.d("ddm-heap", "Sending " + name(CHUNK_HPIF) + ": when=" + when);
/* 151 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleHPST(Client client, ByteBuffer data)
   {
/* 162 */     client.getClientData().getVmHeapData().clearHeapData();
   }
 
   private void handleHPEN(Client client, ByteBuffer data)
   {
/* 173 */     client.getClientData().getVmHeapData().sealHeapData();
/* 174 */     client.update(64);
   }
 
   private void handleHPSG(Client client, ByteBuffer data)
   {
/* 181 */     byte[] dataCopy = new byte[data.limit()];
/* 182 */     data.rewind();
/* 183 */     data.get(dataCopy);
/* 184 */     data = ByteBuffer.wrap(dataCopy);
/* 185 */     client.getClientData().getVmHeapData().addHeapData(data);
   }
 
   public static void sendHPSG(Client client, int when, int what)
     throws IOException
   {
/* 195 */     ByteBuffer rawBuf = allocBuffer(2);
/* 196 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 197 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 199 */     buf.put((byte)when);
/* 200 */     buf.put((byte)what);
 
/* 202 */     finishChunkPacket(packet, CHUNK_HPSG, buf.position());
/* 203 */     Log.d("ddm-heap", "Sending " + name(CHUNK_HPSG) + ": when=" + when + ", what=" + what);
 
/* 205 */     client.sendAndConsume(packet, mInst);
   }
 
   public static void sendHPGC(Client client)
     throws IOException
   {
/* 213 */     ByteBuffer rawBuf = allocBuffer(0);
/* 214 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 215 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 219 */     finishChunkPacket(packet, CHUNK_HPGC, buf.position());
/* 220 */     Log.d("ddm-heap", "Sending " + name(CHUNK_HPGC));
/* 221 */     client.sendAndConsume(packet, mInst);
   }
 
   public static void sendHPDU(Client client, String fileName)
     throws IOException
   {
/* 234 */     ByteBuffer rawBuf = allocBuffer(4 + fileName.length() * 2);
/* 235 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 236 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 238 */     buf.putInt(fileName.length());
/* 239 */     ByteBufferUtil.putString(buf, fileName);
 
/* 241 */     finishChunkPacket(packet, CHUNK_HPDU, buf.position());
/* 242 */     Log.d("ddm-heap", "Sending " + name(CHUNK_HPDU) + " '" + fileName + "'");
/* 243 */     client.sendAndConsume(packet, mInst);
/* 244 */     client.getClientData().setPendingHprofDump(fileName);
   }
 
   public static void sendHPDS(Client client)
     throws IOException
   {
/* 261 */     ByteBuffer rawBuf = allocBuffer(0);
/* 262 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 263 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 265 */     finishChunkPacket(packet, CHUNK_HPDS, buf.position());
/* 266 */     Log.d("ddm-heap", "Sending " + name(CHUNK_HPDS));
/* 267 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleHPDU(Client client, ByteBuffer data)
   {
/* 277 */     String filename = client.getClientData().getPendingHprofDump();
/* 278 */     client.getClientData().setPendingHprofDump(null);
 
/* 281 */     byte result = data.get();
 
/* 284 */     ClientData.IHprofDumpHandler handler = ClientData.getHprofDumpHandler();
/* 285 */     if (result == 0) {
/* 286 */       if (handler != null) {
/* 287 */         handler.onSuccess(filename, client);
       }
/* 289 */       client.getClientData().setHprofData(filename);
/* 290 */       Log.d("ddm-heap", "Heap dump request has finished");
     } else {
/* 292 */       if (handler != null) {
/* 293 */         handler.onEndFailure(client, null);
       }
/* 295 */       client.getClientData().clearHprofData();
/* 296 */       Log.w("ddm-heap", "Heap dump request failed (check device log)");
     }
/* 298 */     client.update(4096);
/* 299 */     client.getClientData().clearHprofData();
   }
 
   private void handleHPDS(Client client, ByteBuffer data)
   {
/* 307 */     byte[] stuff = new byte[data.capacity()];
/* 308 */     data.get(stuff, 0, stuff.length);
 
/* 310 */     Log.d("ddm-hprof", "got hprof file, size: " + data.capacity() + " bytes");
/* 311 */     client.getClientData().setHprofData(stuff);
/* 312 */     ClientData.IHprofDumpHandler handler = ClientData.getHprofDumpHandler();
/* 313 */     if (handler != null) {
/* 314 */       handler.onSuccess(stuff, client);
     }
/* 316 */     client.update(4096);
/* 317 */     client.getClientData().clearHprofData();
   }
 
   public static void sendREAE(Client client, boolean enable)
     throws IOException
   {
/* 325 */     ByteBuffer rawBuf = allocBuffer(1);
/* 326 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 327 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 329 */     buf.put((byte)(enable ? 1 : 0));
 
/* 331 */     finishChunkPacket(packet, CHUNK_REAE, buf.position());
/* 332 */     Log.d("ddm-heap", "Sending " + name(CHUNK_REAE) + ": " + enable);
/* 333 */     client.sendAndConsume(packet, mInst);
   }
 
   public static void sendREAQ(Client client)
     throws IOException
   {
/* 341 */     ByteBuffer rawBuf = allocBuffer(0);
/* 342 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 343 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 347 */     finishChunkPacket(packet, CHUNK_REAQ, buf.position());
/* 348 */     Log.d("ddm-heap", "Sending " + name(CHUNK_REAQ));
/* 349 */     client.sendAndConsume(packet, mInst);
   }
 
   public static void sendREAL(Client client)
     throws IOException
   {
/* 357 */     ByteBuffer rawBuf = allocBuffer(0);
/* 358 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 359 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 363 */     finishChunkPacket(packet, CHUNK_REAL, buf.position());
/* 364 */     Log.d("ddm-heap", "Sending " + name(CHUNK_REAL));
/* 365 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleREAQ(Client client, ByteBuffer data)
   {
/* 374 */     boolean enabled = data.get() != 0;
/* 375 */     Log.d("ddm-heap", "REAQ says: enabled=" + enabled);
 
/* 377 */     client.getClientData().setAllocationStatus(enabled ? ClientData.AllocationTrackingStatus.ON : ClientData.AllocationTrackingStatus.OFF);
/* 378 */     client.update(1024);
   }
 
   private void handleREAL(Client client, ByteBuffer data)
   {
/* 385 */     Log.e("ddm-heap", "*** Received " + name(CHUNK_REAL));
 
/* 387 */     byte[] stuff = new byte[data.capacity()];
/* 388 */     data.get(stuff, 0, stuff.length);
/* 389 */     data.rewind();
 
/* 392 */     ClientData.IAllocationTrackingHandler handler = ClientData.getAllocationTrackingHandler();
/* 393 */     if (handler != null) {
/* 394 */       Log.d("ddm-prof", "got allocations file, size: " + stuff.length + " bytes");
/* 395 */       handler.onSuccess(stuff, client);
     }
 
/* 398 */     client.getClientData().setAllocationsData(stuff);
/* 399 */     client.update(512);
 
/* 402 */     client.getClientData().setAllocationsData(null);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleHeap
 * JD-Core Version:    0.6.2
 */