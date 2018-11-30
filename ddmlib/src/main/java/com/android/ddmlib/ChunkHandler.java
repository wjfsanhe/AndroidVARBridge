 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 
 abstract class ChunkHandler
 {
   public static final int CHUNK_HEADER_LEN = 8;
/*  31 */   public static final ByteOrder CHUNK_ORDER = ByteOrder.BIG_ENDIAN;
 
/*  33 */   public static final int CHUNK_FAIL = type("FAIL");
 
   abstract void clientReady(Client paramClient)
     throws IOException;
 
   abstract void clientDisconnected(Client paramClient);
 
   abstract void handleChunk(Client paramClient, int paramInt1, ByteBuffer paramByteBuffer, boolean paramBoolean, int paramInt2);
 
   protected void handleUnknownChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
   {
/*  74 */     if (type == CHUNK_FAIL)
     {
/*  78 */       int errorCode = data.getInt();
/*  79 */       int msgLen = data.getInt();
/*  80 */       String msg = ByteBufferUtil.getString(data, msgLen);
/*  81 */       Log.w("ddms", "WARNING: failure code=" + errorCode + " msg=" + msg);
     } else {
/*  83 */       Log.w("ddms", "WARNING: received unknown chunk " + name(type) + ": len=" + data.limit() + ", reply=" + isReply + ", msgId=0x" + Integer.toHexString(msgId));
     }
 
/*  87 */     Log.w("ddms", "         client " + client + ", handler " + this);
   }
 
   public static String getString(ByteBuffer buf, int len)
   {
/*  94 */     return ByteBufferUtil.getString(buf, len);
   }
 
   static int type(String typeName)
   {
/* 101 */     int val = 0;
 
/* 103 */     if (typeName.length() != 4) {
/* 104 */       Log.e("ddms", "Type name must be 4 letter long");
/* 105 */       throw new RuntimeException("Type name must be 4 letter long");
     }
 
/* 108 */     for (int i = 0; i < 4; i++) {
/* 109 */       val <<= 8;
/* 110 */       val |= (byte)typeName.charAt(i);
     }
 
/* 113 */     return val;
   }
 
   static String name(int type)
   {
/* 120 */     char[] ascii = new char[4];
 
/* 122 */     ascii[0] = ((char)(type >> 24 & 0xFF));
/* 123 */     ascii[1] = ((char)(type >> 16 & 0xFF));
/* 124 */     ascii[2] = ((char)(type >> 8 & 0xFF));
/* 125 */     ascii[3] = ((char)(type & 0xFF));
 
/* 127 */     return new String(ascii);
   }
 
   static ByteBuffer allocBuffer(int maxChunkLen)
   {
/* 138 */     ByteBuffer buf = ByteBuffer.allocate(19 + maxChunkLen);
 
/* 140 */     buf.order(CHUNK_ORDER);
/* 141 */     return buf;
   }
 
   static ByteBuffer getChunkDataBuf(ByteBuffer jdwpBuf)
   {
/* 151 */     assert (jdwpBuf.position() == 0);
 
/* 153 */     jdwpBuf.position(19);
/* 154 */     ByteBuffer slice = jdwpBuf.slice();
/* 155 */     slice.order(CHUNK_ORDER);
/* 156 */     jdwpBuf.position(0);
 
/* 158 */     return slice;
   }
 
   static void finishChunkPacket(JdwpPacket packet, int type, int chunkLen)
   {
/* 167 */     ByteBuffer buf = packet.getPayload();
 
/* 169 */     buf.putInt(0, type);
/* 170 */     buf.putInt(4, chunkLen);
 
/* 172 */     packet.finishPacket(8 + chunkLen);
   }
 
   protected static Client checkDebuggerPortForAppName(Client client, String appName)
   {
/* 184 */     DebugPortManager.IDebugPortProvider provider = DebugPortManager.getProvider();
/* 185 */     if (provider != null) {
/* 186 */       Device device = client.getDeviceImpl();
/* 187 */       int newPort = provider.getPort(device, appName);
 
/* 189 */       if ((newPort != -1) && (newPort != client.getDebuggerListenPort()))
       {
/* 192 */         AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
/* 193 */         if (bridge != null) {
/* 194 */           DeviceMonitor deviceMonitor = bridge.getDeviceMonitor();
/* 195 */           if (deviceMonitor != null) {
/* 196 */             deviceMonitor.addClientToDropAndReopen(client, newPort);
/* 197 */             client = null;
           }
         }
       }
     }
 
/* 203 */     return client;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.ChunkHandler
 * JD-Core Version:    0.6.2
 */