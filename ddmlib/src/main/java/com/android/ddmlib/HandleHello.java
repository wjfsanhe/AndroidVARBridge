 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.BufferUnderflowException;
 import java.nio.ByteBuffer;
 
 final class HandleHello extends ChunkHandler
 {
/*  28 */   public static final int CHUNK_HELO = ChunkHandler.type("HELO");
/*  29 */   public static final int CHUNK_FEAT = ChunkHandler.type("FEAT");
 
/*  31 */   private static final HandleHello mInst = new HandleHello();
 
   public static void register(MonitorThread mt)
   {
/*  39 */     mt.registerChunkHandler(CHUNK_HELO, mInst);
   }
 
   public void clientReady(Client client)
     throws IOException
   {
/*  47 */     Log.d("ddm-hello", "Now ready: " + client);
   }
 
   public void clientDisconnected(Client client)
   {
/*  55 */     Log.d("ddm-hello", "Now disconnected: " + client);
   }
 
   public static void sendHelloCommands(Client client, int serverProtocolVersion)
     throws IOException
   {
/*  66 */     sendHELO(client, serverProtocolVersion);
/*  67 */     sendFEAT(client);
/*  68 */     HandleProfiling.sendMPRQ(client);
   }
 
   public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
   {
/*  77 */     Log.d("ddm-hello", "handling " + ChunkHandler.name(type));
 
/*  79 */     if (type == CHUNK_HELO) {
/*  80 */       assert (isReply);
/*  81 */       handleHELO(client, data);
/*  82 */     } else if (type == CHUNK_FEAT) {
/*  83 */       handleFEAT(client, data);
     } else {
/*  85 */       handleUnknownChunk(client, type, data, isReply, msgId);
     }
   }
 
   private static void handleHELO(Client client, ByteBuffer data)
   {
/*  96 */     int version = data.getInt();
/*  97 */     int pid = data.getInt();
/*  98 */     int vmIdentLen = data.getInt();
/*  99 */     int appNameLen = data.getInt();
 
/* 101 */     String vmIdent = ByteBufferUtil.getString(data, vmIdentLen);
/* 102 */     String appName = ByteBufferUtil.getString(data, appNameLen);
 
/* 105 */     int userId = -1;
/* 106 */     boolean validUserId = false;
/* 107 */     if (data.hasRemaining()) {
       try {
/* 109 */         userId = data.getInt();
/* 110 */         validUserId = true;
       }
       catch (BufferUnderflowException e) {
/* 113 */         int expectedPacketLength = 20 + appNameLen * 2 + vmIdentLen * 2;
 
/* 115 */         Log.e("ddm-hello", "Insufficient data in HELO chunk to retrieve user id.");
/* 116 */         Log.e("ddm-hello", "Actual chunk length: " + data.capacity());
/* 117 */         Log.e("ddm-hello", "Expected chunk length: " + expectedPacketLength);
       }
 
     }
 
/* 122 */     boolean validAbi = false;
/* 123 */     String abi = null;
/* 124 */     if (data.hasRemaining()) {
       try {
/* 126 */         int abiLength = data.getInt();
/* 127 */         abi = ByteBufferUtil.getString(data, abiLength);
/* 128 */         validAbi = true;
       } catch (BufferUnderflowException e) {
/* 130 */         Log.e("ddm-hello", "Insufficient data in HELO chunk to retrieve ABI.");
       }
     }
 
/* 134 */     boolean hasJvmFlags = false;
/* 135 */     String jvmFlags = null;
/* 136 */     if (data.hasRemaining()) {
       try {
/* 138 */         int jvmFlagsLength = data.getInt();
/* 139 */         jvmFlags = ByteBufferUtil.getString(data, jvmFlagsLength);
/* 140 */         hasJvmFlags = true;
       } catch (BufferUnderflowException e) {
/* 142 */         Log.e("ddm-hello", "Insufficient data in HELO chunk to retrieve JVM flags");
       }
     }
 
/* 146 */     Log.d("ddm-hello", "HELO: v=" + version + ", pid=" + pid + ", vm='" + vmIdent + "', app='" + appName + "'");
 
/* 149 */     ClientData cd = client.getClientData();
 
/* 151 */     if (cd.getPid() == pid) {
/* 152 */       cd.setVmIdentifier(vmIdent);
/* 153 */       cd.setClientDescription(appName);
/* 154 */       cd.isDdmAware(true);
 
/* 156 */       if (validUserId) {
/* 157 */         cd.setUserId(userId);
       }
 
/* 160 */       if (validAbi) {
/* 161 */         cd.setAbi(abi);
       }
 
/* 164 */       if (hasJvmFlags)
/* 165 */         cd.setJvmFlags(jvmFlags);
     }
     else {
/* 168 */       Log.e("ddm-hello", "Received pid (" + pid + ") does not match client pid (" + cd.getPid() + ")");
     }
 
/* 172 */     client = checkDebuggerPortForAppName(client, appName);
 
/* 174 */     if (client != null)
/* 175 */       client.update(1);
   }
 
   public static void sendHELO(Client client, int serverProtocolVersion)
     throws IOException
   {
/* 186 */     ByteBuffer rawBuf = allocBuffer(4);
/* 187 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 188 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 190 */     buf.putInt(serverProtocolVersion);
 
/* 192 */     finishChunkPacket(packet, CHUNK_HELO, buf.position());
/* 193 */     Log.d("ddm-hello", "Sending " + name(CHUNK_HELO) + " ID=0x" + Integer.toHexString(packet.getId()));
 
/* 195 */     client.sendAndConsume(packet, mInst);
   }
 
   private static void handleFEAT(Client client, ByteBuffer data)
   {
/* 205 */     int featureCount = data.getInt();
/* 206 */     for (int i = 0; i < featureCount; i++) {
/* 207 */       int len = data.getInt();
/* 208 */       String feature = ByteBufferUtil.getString(data, len);
/* 209 */       client.getClientData().addFeature(feature);
 
/* 211 */       Log.d("ddm-hello", "Feature: " + feature);
     }
   }
 
   public static void sendFEAT(Client client)
     throws IOException
   {
/* 219 */     ByteBuffer rawBuf = allocBuffer(0);
/* 220 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 221 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 225 */     finishChunkPacket(packet, CHUNK_FEAT, buf.position());
/* 226 */     Log.d("ddm-heap", "Sending " + name(CHUNK_FEAT));
/* 227 */     client.sendAndConsume(packet, mInst);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleHello
 * JD-Core Version:    0.6.2
 */