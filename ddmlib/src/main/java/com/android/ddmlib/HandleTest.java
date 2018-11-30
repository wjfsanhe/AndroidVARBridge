 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 
 final class HandleTest extends ChunkHandler
 {
/* 29 */   public static final int CHUNK_TEST = type("TEST");
 
/* 31 */   private static final HandleTest mInst = new HandleTest();
 
   public static void register(MonitorThread mt)
   {
/* 40 */     mt.registerChunkHandler(CHUNK_TEST, mInst);
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
/* 61 */     Log.d("ddm-test", "handling " + ChunkHandler.name(type));
 
/* 63 */     if (type == CHUNK_TEST)
/* 64 */       handleTEST(client, data);
     else
/* 66 */       handleUnknownChunk(client, type, data, isReply, msgId);
   }
 
   private void handleTEST(Client client, ByteBuffer data)
   {
/* 79 */     byte[] copy = new byte[data.limit()];
/* 80 */     data.get(copy);
 
/* 82 */     Log.d("ddm-test", "Received:");
/* 83 */     Log.hexDump("ddm-test", Log.LogLevel.DEBUG, copy, 0, copy.length);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleTest
 * JD-Core Version:    0.6.2
 */