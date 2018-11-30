 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.concurrent.TimeUnit;
 
 final class HandleProfiling extends ChunkHandler
 {
/*  31 */   public static final int CHUNK_MPRS = type("MPRS");
/*  32 */   public static final int CHUNK_MPRE = type("MPRE");
/*  33 */   public static final int CHUNK_MPSS = type("MPSS");
/*  34 */   public static final int CHUNK_MPSE = type("MPSE");
/*  35 */   public static final int CHUNK_SPSS = type("SPSS");
/*  36 */   public static final int CHUNK_SPSE = type("SPSE");
/*  37 */   public static final int CHUNK_MPRQ = type("MPRQ");
/*  38 */   public static final int CHUNK_FAIL = type("FAIL");
 
/*  40 */   private static final HandleProfiling mInst = new HandleProfiling();
 
   public static void register(MonitorThread mt)
   {
/*  48 */     mt.registerChunkHandler(CHUNK_MPRE, mInst);
/*  49 */     mt.registerChunkHandler(CHUNK_MPSE, mInst);
/*  50 */     mt.registerChunkHandler(CHUNK_MPRQ, mInst);
   }
 
   public void clientReady(Client client)
     throws IOException
   {
   }
 
   public void clientDisconnected(Client client)
   {
   }
 
   public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
   {
/*  72 */     Log.d("ddm-prof", "handling " + ChunkHandler.name(type));
 
/*  74 */     if (type == CHUNK_MPRE)
/*  75 */       handleMPRE(client, data);
/*  76 */     else if (type == CHUNK_MPSE)
/*  77 */       handleMPSE(client, data);
/*  78 */     else if (type == CHUNK_MPRQ)
/*  79 */       handleMPRQ(client, data);
/*  80 */     else if (type == CHUNK_FAIL)
/*  81 */       handleFAIL(client, data);
     else
/*  83 */       handleUnknownChunk(client, type, data, isReply, msgId);
   }
 
   public static void sendMPRS(Client client, String fileName, int bufferSize, int flags)
     throws IOException
   {
/* 102 */     ByteBuffer rawBuf = allocBuffer(12 + fileName.length() * 2);
/* 103 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 104 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 106 */     buf.putInt(bufferSize);
/* 107 */     buf.putInt(flags);
/* 108 */     buf.putInt(fileName.length());
/* 109 */     ByteBufferUtil.putString(buf, fileName);
 
/* 111 */     finishChunkPacket(packet, CHUNK_MPRS, buf.position());
/* 112 */     Log.d("ddm-prof", "Sending " + name(CHUNK_MPRS) + " '" + fileName + "', size=" + bufferSize + ", flags=" + flags);
 
/* 114 */     client.sendAndConsume(packet, mInst);
 
/* 117 */     client.getClientData().setPendingMethodProfiling(fileName);
 
/* 121 */     sendMPRQ(client);
   }
 
   public static void sendMPRE(Client client)
     throws IOException
   {
/* 128 */     ByteBuffer rawBuf = allocBuffer(0);
/* 129 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 130 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 134 */     finishChunkPacket(packet, CHUNK_MPRE, buf.position());
/* 135 */     Log.d("ddm-prof", "Sending " + name(CHUNK_MPRE));
/* 136 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleMPRE(Client client, ByteBuffer data)
   {
/* 147 */     String filename = client.getClientData().getPendingMethodProfiling();
/* 148 */     client.getClientData().setPendingMethodProfiling(null);
 
/* 150 */     byte result = data.get();
 
/* 153 */     ClientData.IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
/* 154 */     if (handler != null) {
/* 155 */       if (result == 0) {
/* 156 */         handler.onSuccess(filename, client);
 
/* 158 */         Log.d("ddm-prof", "Method profiling has finished");
       } else {
/* 160 */         handler.onEndFailure(client, null);
 
/* 162 */         Log.w("ddm-prof", "Method profiling has failed (check device log)");
       }
     }
 
/* 166 */     client.getClientData().setMethodProfilingStatus(ClientData.MethodProfilingStatus.OFF);
/* 167 */     client.update(2048);
   }
 
   public static void sendMPSS(Client client, int bufferSize, int flags)
     throws IOException
   {
/* 182 */     ByteBuffer rawBuf = allocBuffer(8);
/* 183 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 184 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 186 */     buf.putInt(bufferSize);
/* 187 */     buf.putInt(flags);
 
/* 189 */     finishChunkPacket(packet, CHUNK_MPSS, buf.position());
/* 190 */     Log.d("ddm-prof", "Sending " + name(CHUNK_MPSS) + "', size=" + bufferSize + ", flags=" + flags);
 
/* 192 */     client.sendAndConsume(packet, mInst);
 
/* 196 */     sendMPRQ(client);
   }
 
   public static void sendSPSS(Client client, int bufferSize, int samplingInterval, TimeUnit samplingIntervalTimeUnits)
     throws IOException
   {
/* 208 */     int interval = (int)samplingIntervalTimeUnits.toMicros(samplingInterval);
 
/* 210 */     ByteBuffer rawBuf = allocBuffer(12);
/* 211 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 212 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 214 */     buf.putInt(bufferSize);
/* 215 */     buf.putInt(0);
/* 216 */     buf.putInt(interval);
 
/* 218 */     finishChunkPacket(packet, CHUNK_SPSS, buf.position());
/* 219 */     Log.d("ddm-prof", "Sending " + name(CHUNK_SPSS) + "', size=" + bufferSize + ", flags=0, samplingInterval=" + interval);
 
/* 221 */     client.sendAndConsume(packet, mInst);
 
/* 225 */     sendMPRQ(client);
   }
 
   public static void sendMPSE(Client client)
     throws IOException
   {
/* 232 */     ByteBuffer rawBuf = allocBuffer(0);
/* 233 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 234 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 238 */     finishChunkPacket(packet, CHUNK_MPSE, buf.position());
/* 239 */     Log.d("ddm-prof", "Sending " + name(CHUNK_MPSE));
/* 240 */     client.sendAndConsume(packet, mInst);
   }
 
   public static void sendSPSE(Client client)
     throws IOException
   {
/* 247 */     ByteBuffer rawBuf = allocBuffer(0);
/* 248 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 249 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 253 */     finishChunkPacket(packet, CHUNK_SPSE, buf.position());
/* 254 */     Log.d("ddm-prof", "Sending " + name(CHUNK_SPSE));
/* 255 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleMPSE(Client client, ByteBuffer data)
   {
/* 263 */     ClientData.IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
/* 264 */     if (handler != null) {
/* 265 */       byte[] stuff = new byte[data.capacity()];
/* 266 */       data.get(stuff, 0, stuff.length);
 
/* 268 */       Log.d("ddm-prof", "got trace file, size: " + stuff.length + " bytes");
 
/* 270 */       handler.onSuccess(stuff, client);
     }
 
/* 273 */     client.getClientData().setMethodProfilingStatus(ClientData.MethodProfilingStatus.OFF);
/* 274 */     client.update(2048);
   }
 
   public static void sendMPRQ(Client client)
     throws IOException
   {
/* 281 */     ByteBuffer rawBuf = allocBuffer(0);
/* 282 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 283 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 287 */     finishChunkPacket(packet, CHUNK_MPRQ, buf.position());
/* 288 */     Log.d("ddm-prof", "Sending " + name(CHUNK_MPRQ));
/* 289 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleMPRQ(Client client, ByteBuffer data)
   {
/* 298 */     byte result = data.get();
 
/* 300 */     if (result == 0) {
/* 301 */       client.getClientData().setMethodProfilingStatus(ClientData.MethodProfilingStatus.OFF);
/* 302 */       Log.d("ddm-prof", "Method profiling is not running");
/* 303 */     } else if (result == 1) {
/* 304 */       client.getClientData().setMethodProfilingStatus(ClientData.MethodProfilingStatus.TRACER_ON);
/* 305 */       Log.d("ddm-prof", "Method tracing is active");
/* 306 */     } else if (result == 2) {
/* 307 */       client.getClientData().setMethodProfilingStatus(ClientData.MethodProfilingStatus.SAMPLER_ON);
/* 308 */       Log.d("ddm-prof", "Sampler based profiling is active");
     }
/* 310 */     client.update(2048);
   }
 
   private void handleFAIL(Client client, ByteBuffer data) {
/* 314 */     data.getInt();
/* 315 */     int length = data.getInt() * 2;
/* 316 */     String message = null;
/* 317 */     if (length > 0) {
/* 318 */       byte[] messageBuffer = new byte[length];
/* 319 */       data.get(messageBuffer, 0, length);
/* 320 */       message = new String(messageBuffer);
     }
 
/* 327 */     String filename = client.getClientData().getPendingMethodProfiling();
/* 328 */     if (filename != null)
     {
/* 330 */       client.getClientData().setPendingMethodProfiling(null);
 
/* 333 */       ClientData.IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
/* 334 */       if (handler != null) {
/* 335 */         handler.onStartFailure(client, message);
       }
     }
     else
     {
/* 340 */       ClientData.IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
/* 341 */       if (handler != null) {
/* 342 */         handler.onEndFailure(client, message);
       }
     }
 
     try
     {
/* 348 */       sendMPRQ(client);
     } catch (IOException e) {
/* 350 */       Log.e("HandleProfiling", e);
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleProfiling
 * JD-Core Version:    0.6.2
 */