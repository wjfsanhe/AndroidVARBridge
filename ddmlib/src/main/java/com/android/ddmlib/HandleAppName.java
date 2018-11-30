 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.BufferUnderflowException;
 import java.nio.ByteBuffer;
 
 final class HandleAppName extends ChunkHandler
 {
/*  28 */   public static final int CHUNK_APNM = ChunkHandler.type("APNM");
 
/*  30 */   private static final HandleAppName mInst = new HandleAppName();
 
   public static void register(MonitorThread mt)
   {
/*  39 */     mt.registerChunkHandler(CHUNK_APNM, mInst);
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
/*  61 */     Log.d("ddm-appname", "handling " + ChunkHandler.name(type));
 
/*  63 */     if (type == CHUNK_APNM) {
/*  64 */       assert (!isReply);
/*  65 */       handleAPNM(client, data);
     } else {
/*  67 */       handleUnknownChunk(client, type, data, isReply, msgId);
     }
   }
 
   private static void handleAPNM(Client client, ByteBuffer data)
   {
/*  78 */     int appNameLen = data.getInt();
/*  79 */     String appName = ByteBufferUtil.getString(data, appNameLen);
 
/*  82 */     int userId = -1;
/*  83 */     boolean validUserId = false;
/*  84 */     if (data.hasRemaining()) {
       try {
/*  86 */         userId = data.getInt();
/*  87 */         validUserId = true;
       }
       catch (BufferUnderflowException e) {
/*  90 */         int expectedPacketLength = 8 + appNameLen * 2;
 
/*  92 */         Log.e("ddm-appname", "Insufficient data in APNM chunk to retrieve user id.");
/*  93 */         Log.e("ddm-appname", "Actual chunk length: " + data.capacity());
/*  94 */         Log.e("ddm-appname", "Expected chunk length: " + expectedPacketLength);
       }
     }
 
/*  98 */     Log.d("ddm-appname", "APNM: app='" + appName + "'");
 
/* 100 */     ClientData cd = client.getClientData();
/* 101 */     synchronized (cd) {
/* 102 */       cd.setClientDescription(appName);
 
/* 104 */       if (validUserId) {
/* 105 */         cd.setUserId(userId);
       }
     }
 
/* 109 */     client = checkDebuggerPortForAppName(client, appName);
 
/* 111 */     if (client != null)
/* 112 */       client.update(1);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleAppName
 * JD-Core Version:    0.6.2
 */