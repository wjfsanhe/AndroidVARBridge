 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 
 final class HandleWait extends ChunkHandler
 {
/* 30 */   public static final int CHUNK_WAIT = ChunkHandler.type("WAIT");
 
/* 32 */   private static final HandleWait mInst = new HandleWait();
 
   public static void register(MonitorThread mt)
   {
/* 41 */     mt.registerChunkHandler(CHUNK_WAIT, mInst);
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
/* 62 */     Log.d("ddm-wait", "handling " + ChunkHandler.name(type));
 
/* 64 */     if (type == CHUNK_WAIT) {
/* 65 */       assert (!isReply);
/* 66 */       handleWAIT(client, data);
     } else {
/* 68 */       handleUnknownChunk(client, type, data, isReply, msgId);
     }
   }
 
   private static void handleWAIT(Client client, ByteBuffer data)
   {
/* 78 */     byte reason = data.get();
 
/* 80 */     Log.d("ddm-wait", "WAIT: reason=" + reason);
 
/* 83 */     ClientData cd = client.getClientData();
/* 84 */     synchronized (cd) {
/* 85 */       cd.setDebuggerConnectionStatus(ClientData.DebuggerStatus.WAITING);
     }
 
/* 88 */     client.update(2);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleWait
 * JD-Core Version:    0.6.2
 */