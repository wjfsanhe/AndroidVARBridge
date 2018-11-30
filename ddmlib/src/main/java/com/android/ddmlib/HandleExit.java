 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 
 final class HandleExit extends ChunkHandler
 {
/* 27 */   public static final int CHUNK_EXIT = type("EXIT");
 
/* 29 */   private static final HandleExit mInst = new HandleExit();
 
   public static void register(MonitorThread mt)
   {
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
/* 56 */     handleUnknownChunk(client, type, data, isReply, msgId);
   }
 
   public static void sendEXIT(Client client, int status)
     throws IOException
   {
/* 65 */     ByteBuffer rawBuf = allocBuffer(4);
/* 66 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 67 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 69 */     buf.putInt(status);
 
/* 71 */     finishChunkPacket(packet, CHUNK_EXIT, buf.position());
/* 72 */     Log.d("ddm-exit", "Sending " + name(CHUNK_EXIT) + ": " + status);
/* 73 */     client.sendAndConsume(packet, mInst);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleExit
 * JD-Core Version:    0.6.2
 */