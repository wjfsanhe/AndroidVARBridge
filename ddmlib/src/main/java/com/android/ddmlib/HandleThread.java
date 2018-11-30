 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 
 final class HandleThread extends ChunkHandler
 {
/*  27 */   public static final int CHUNK_THEN = type("THEN");
/*  28 */   public static final int CHUNK_THCR = type("THCR");
/*  29 */   public static final int CHUNK_THDE = type("THDE");
/*  30 */   public static final int CHUNK_THST = type("THST");
/*  31 */   public static final int CHUNK_THNM = type("THNM");
/*  32 */   public static final int CHUNK_STKL = type("STKL");
 
/*  34 */   private static final HandleThread mInst = new HandleThread();
 
/*  37 */   private static volatile boolean sThreadStatusReqRunning = false;
/*  38 */   private static volatile boolean sThreadStackTraceReqRunning = false;
 
   public static void register(MonitorThread mt)
   {
/*  47 */     mt.registerChunkHandler(CHUNK_THCR, mInst);
/*  48 */     mt.registerChunkHandler(CHUNK_THDE, mInst);
/*  49 */     mt.registerChunkHandler(CHUNK_THST, mInst);
/*  50 */     mt.registerChunkHandler(CHUNK_THNM, mInst);
/*  51 */     mt.registerChunkHandler(CHUNK_STKL, mInst);
   }
 
   public void clientReady(Client client)
     throws IOException
   {
/*  59 */     Log.d("ddm-thread", "Now ready: " + client);
/*  60 */     if (client.isThreadUpdateEnabled())
/*  61 */       sendTHEN(client, true);
   }
 
   public void clientDisconnected(Client client)
   {
   }
 
   public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
   {
/*  76 */     Log.d("ddm-thread", "handling " + ChunkHandler.name(type));
 
/*  78 */     if (type == CHUNK_THCR)
/*  79 */       handleTHCR(client, data);
/*  80 */     else if (type == CHUNK_THDE)
/*  81 */       handleTHDE(client, data);
/*  82 */     else if (type == CHUNK_THST)
/*  83 */       handleTHST(client, data);
/*  84 */     else if (type == CHUNK_THNM)
/*  85 */       handleTHNM(client, data);
/*  86 */     else if (type == CHUNK_STKL)
/*  87 */       handleSTKL(client, data);
     else
/*  89 */       handleUnknownChunk(client, type, data, isReply, msgId);
   }
 
   private void handleTHCR(Client client, ByteBuffer data)
   {
/* 103 */     int threadId = data.getInt();
/* 104 */     int nameLen = data.getInt();
/* 105 */     String name = ByteBufferUtil.getString(data, nameLen);
 
/* 107 */     Log.v("ddm-thread", "THCR: " + threadId + " '" + name + "'");
 
/* 109 */     client.getClientData().addThread(threadId, name);
/* 110 */     client.update(16);
   }
 
   private void handleTHDE(Client client, ByteBuffer data)
   {
/* 119 */     int threadId = data.getInt();
/* 120 */     Log.v("ddm-thread", "THDE: " + threadId);
 
/* 122 */     client.getClientData().removeThread(threadId);
/* 123 */     client.update(16);
   }
 
   private void handleTHST(Client client, ByteBuffer data)
   {
/* 144 */     int headerLen = data.get() & 0xFF;
/* 145 */     int bytesPerEntry = data.get() & 0xFF;
/* 146 */     int threadCount = data.getShort();
 
/* 148 */     headerLen -= 4;
/* 149 */     while (headerLen-- > 0) {
/* 150 */       data.get();
     }
/* 152 */     int extraPerEntry = bytesPerEntry - 18;
 
/* 154 */     Log.v("ddm-thread", "THST: threadCount=" + threadCount);
 
/* 160 */     for (int i = 0; i < threadCount; i++)
     {
/* 162 */       boolean isDaemon = false;
 
/* 164 */       int threadId = data.getInt();
/* 165 */       int status = data.get();
/* 166 */       int tid = data.getInt();
/* 167 */       int utime = data.getInt();
/* 168 */       int stime = data.getInt();
/* 169 */       if (bytesPerEntry >= 18) {
/* 170 */         isDaemon = data.get() != 0;
       }
/* 172 */       Log.v("ddm-thread", "  id=" + threadId + ", status=" + status + ", tid=" + tid + ", utime=" + utime + ", stime=" + stime);
 
/* 176 */       ClientData cd = client.getClientData();
/* 177 */       ThreadInfo threadInfo = cd.getThread(threadId);
/* 178 */       if (threadInfo != null)
/* 179 */         threadInfo.updateThread(status, tid, utime, stime, isDaemon);
       else {
/* 181 */         Log.d("ddms", "Thread with id=" + threadId + " not found");
       }
 
/* 184 */       for (int slurp = extraPerEntry; slurp > 0; slurp--) {
/* 185 */         data.get();
       }
     }
/* 188 */     client.update(16);
   }
 
   private void handleTHNM(Client client, ByteBuffer data)
   {
/* 199 */     int threadId = data.getInt();
/* 200 */     int nameLen = data.getInt();
/* 201 */     String name = ByteBufferUtil.getString(data, nameLen);
 
/* 203 */     Log.v("ddm-thread", "THNM: " + threadId + " '" + name + "'");
 
/* 205 */     ThreadInfo threadInfo = client.getClientData().getThread(threadId);
/* 206 */     if (threadInfo != null) {
/* 207 */       threadInfo.setThreadName(name);
/* 208 */       client.update(16);
     } else {
/* 210 */       Log.d("ddms", "Thread with id=" + threadId + " not found");
     }
   }
 
   private void handleSTKL(Client client, ByteBuffer data)
   {
/* 224 */     int future = data.getInt();
/* 225 */     int threadId = data.getInt();
 
/* 227 */     Log.v("ddms", "STKL: " + threadId);
 
/* 230 */     int stackDepth = data.getInt();
/* 231 */     StackTraceElement[] trace = new StackTraceElement[stackDepth];
/* 232 */     for (int i = 0; i < stackDepth; i++)
     {
/* 236 */       int len = data.getInt();
/* 237 */       String className = ByteBufferUtil.getString(data, len);
/* 238 */       len = data.getInt();
/* 239 */       String methodName = ByteBufferUtil.getString(data, len);
/* 240 */       len = data.getInt();
       String fileName;
/* 241 */       if (len == 0)
/* 242 */         fileName = null;
       else {
/* 244 */         fileName = ByteBufferUtil.getString(data, len);
       }
/* 246 */       int lineNumber = data.getInt();
 
/* 248 */       trace[i] = new StackTraceElement(className, methodName, fileName, lineNumber);
     }
 
/* 252 */     ThreadInfo threadInfo = client.getClientData().getThread(threadId);
/* 253 */     if (threadInfo != null) {
/* 254 */       threadInfo.setStackCall(trace);
/* 255 */       client.update(256);
     } else {
/* 257 */       Log.d("STKL", String.format("Got stackcall for thread %1$d, which does not exists (anymore?).", new Object[] { Integer.valueOf(threadId) }));
     }
   }
 
   public static void sendTHEN(Client client, boolean enable)
     throws IOException
   {
/* 270 */     ByteBuffer rawBuf = allocBuffer(1);
/* 271 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 272 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 274 */     if (enable)
/* 275 */       buf.put((byte)1);
     else {
/* 277 */       buf.put((byte)0);
     }
/* 279 */     finishChunkPacket(packet, CHUNK_THEN, buf.position());
/* 280 */     Log.d("ddm-thread", "Sending " + name(CHUNK_THEN) + ": " + enable);
/* 281 */     client.sendAndConsume(packet, mInst);
   }
 
   public static void sendSTKL(Client client, int threadId)
     throws IOException
   {
/* 298 */     ByteBuffer rawBuf = allocBuffer(4);
/* 299 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 300 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 302 */     buf.putInt(threadId);
 
/* 304 */     finishChunkPacket(packet, CHUNK_STKL, buf.position());
/* 305 */     Log.d("ddm-thread", "Sending " + name(CHUNK_STKL) + ": " + threadId);
/* 306 */     client.sendAndConsume(packet, mInst);
   }
 
   static void requestThreadUpdate(final Client client)
   {
/* 316 */     if ((client.isDdmAware()) && (client.isThreadUpdateEnabled())) {
/* 317 */       if (sThreadStatusReqRunning) {
/* 318 */         Log.w("ddms", "Waiting for previous thread update req to finish");
/* 319 */         return;
       }
 
/* 322 */       new Thread("Thread Status Req")
       {
         public void run() {
/* 325 */          sThreadStatusReqRunning = true;
           try {
/* 327 */             HandleThread.sendTHST(client);
           } catch (IOException ioe) {
/* 329 */             Log.d("ddms", "Unable to request thread updates from " + client + ": " + ioe.getMessage());
           }
           finally {
/* 332 */            sThreadStatusReqRunning = false;
           }
         }
       }
/* 322 */       .start();
     }
   }
 
   static void requestThreadStackCallRefresh(final Client client, final int threadId)
   {
/* 340 */     if ((client.isDdmAware()) && (client.isThreadUpdateEnabled())) {
/* 341 */       if (sThreadStackTraceReqRunning) {
/* 342 */         Log.w("ddms", "Waiting for previous thread stack call req to finish");
/* 343 */         return;
       }
 
/* 346 */       new Thread("Thread Status Req")
       {
         public void run() {
/* 349 */           sThreadStackTraceReqRunning = true;
           try {
/* 351 */             HandleThread.sendSTKL(client, threadId);
           } catch (IOException ioe) {
/* 353 */             Log.d("ddms", "Unable to request thread stack call updates from " + client + ": " + ioe.getMessage());
           }
           finally {
/* 356 */             sThreadStackTraceReqRunning = false;
           }
         }
       }
/* 346 */       .start();
     }
   }
 
   private static void sendTHST(Client client)
     throws IOException
   {
/* 368 */     ByteBuffer rawBuf = allocBuffer(0);
/* 369 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 370 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 374 */     finishChunkPacket(packet, CHUNK_THST, buf.position());
/* 375 */     Log.d("ddm-thread", "Sending " + name(CHUNK_THST));
/* 376 */     client.sendAndConsume(packet, mInst);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleThread
 * JD-Core Version:    0.6.2
 */