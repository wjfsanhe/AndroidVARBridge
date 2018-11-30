 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.TimeUnit;
 
 public final class HandleViewDebug extends ChunkHandler
 {
/*  29 */   public static final int CHUNK_VUGL = type("VUGL");
 
/*  32 */   public static final int CHUNK_VULW = type("VULW");
 
/*  35 */   public static final int CHUNK_VURT = type("VURT");
   private static final int VURT_DUMP_HIERARCHY = 1;
   private static final int VURT_CAPTURE_LAYERS = 2;
   private static final int VURT_DUMP_THEME = 3;
/*  50 */   public static final int CHUNK_VUOP = type("VUOP");
   private static final int VUOP_CAPTURE_VIEW = 1;
   private static final int VUOP_DUMP_DISPLAYLIST = 2;
   private static final int VUOP_PROFILE_VIEW = 3;
   private static final int VUOP_INVOKE_VIEW_METHOD = 4;
   private static final int VUOP_SET_LAYOUT_PARAMETER = 5;
   private static final String TAG = "ddmlib";
/*  69 */   private static final HandleViewDebug sInstance = new HandleViewDebug();
 
/*  71 */   private static final ViewDumpHandler sViewOpNullChunkHandler = new NullChunkHandler(CHUNK_VUOP);
 
   public static void register(MonitorThread mt)
   {
/*  79 */     mt.registerChunkHandler(CHUNK_VUGL, sInstance);
/*  80 */     mt.registerChunkHandler(CHUNK_VULW, sInstance);
/*  81 */     mt.registerChunkHandler(CHUNK_VUOP, sInstance);
/*  82 */     mt.registerChunkHandler(CHUNK_VURT, sInstance);
   }
 
   public void clientReady(Client client)
     throws IOException
   {
   }
 
   public void clientDisconnected(Client client)
   {
   }
 
   public static void listViewRoots(Client client, ViewDumpHandler replyHandler)
     throws IOException
   {
/* 132 */     ByteBuffer buf = allocBuffer(8);
/* 133 */     JdwpPacket packet = new JdwpPacket(buf);
/* 134 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
/* 135 */     chunkBuf.putInt(1);
/* 136 */     finishChunkPacket(packet, CHUNK_VULW, chunkBuf.position());
/* 137 */     client.sendAndConsume(packet, replyHandler);
   }
 
   public static void dumpViewHierarchy(Client client, String viewRoot, boolean skipChildren, boolean includeProperties, ViewDumpHandler handler)
     throws IOException
   {
/* 143 */     ByteBuffer buf = allocBuffer(8 + viewRoot.length() * 2 + 4 + 4);
 
/* 148 */     JdwpPacket packet = new JdwpPacket(buf);
/* 149 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
 
/* 151 */     chunkBuf.putInt(1);
/* 152 */     chunkBuf.putInt(viewRoot.length());
/* 153 */     ByteBufferUtil.putString(chunkBuf, viewRoot);
/* 154 */     chunkBuf.putInt(skipChildren ? 1 : 0);
/* 155 */     chunkBuf.putInt(includeProperties ? 1 : 0);
 
/* 157 */     finishChunkPacket(packet, CHUNK_VURT, chunkBuf.position());
/* 158 */     client.sendAndConsume(packet, handler);
   }
 
   public static void captureLayers(Client client, String viewRoot, ViewDumpHandler handler) throws IOException
   {
/* 163 */     int bufLen = 8 + viewRoot.length() * 2;
 
/* 165 */     ByteBuffer buf = allocBuffer(bufLen);
/* 166 */     JdwpPacket packet = new JdwpPacket(buf);
/* 167 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
 
/* 169 */     chunkBuf.putInt(2);
/* 170 */     chunkBuf.putInt(viewRoot.length());
/* 171 */     ByteBufferUtil.putString(chunkBuf, viewRoot);
 
/* 173 */     finishChunkPacket(packet, CHUNK_VURT, chunkBuf.position());
/* 174 */     client.sendAndConsume(packet, handler);
   }
 
   private static void sendViewOpPacket(Client client, int op, String viewRoot, String view, byte[] extra, ViewDumpHandler handler)
     throws IOException
   {
/* 180 */     int bufLen = 8 + viewRoot.length() * 2 + 4 + view.length() * 2;
 
/* 184 */     if (extra != null) {
/* 185 */       bufLen += extra.length;
     }
 
/* 188 */     ByteBuffer buf = allocBuffer(bufLen);
/* 189 */     JdwpPacket packet = new JdwpPacket(buf);
/* 190 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
 
/* 192 */     chunkBuf.putInt(op);
/* 193 */     chunkBuf.putInt(viewRoot.length());
/* 194 */     ByteBufferUtil.putString(chunkBuf, viewRoot);
 
/* 196 */     chunkBuf.putInt(view.length());
/* 197 */     ByteBufferUtil.putString(chunkBuf, view);
 
/* 199 */     if (extra != null) {
/* 200 */       chunkBuf.put(extra);
     }
 
/* 203 */     finishChunkPacket(packet, CHUNK_VUOP, chunkBuf.position());
/* 204 */     if (handler != null)
/* 205 */       client.sendAndConsume(packet, handler);
     else
/* 207 */       client.sendAndConsume(packet);
   }
 
   public static void profileView(Client client, String viewRoot, String view, ViewDumpHandler handler)
     throws IOException
   {
/* 213 */     sendViewOpPacket(client, 3, viewRoot, view, null, handler);
   }
 
   public static void captureView(Client client, String viewRoot, String view, ViewDumpHandler handler) throws IOException
   {
/* 218 */     sendViewOpPacket(client, 1, viewRoot, view, null, handler);
   }
 
   public static void invalidateView(Client client, String viewRoot, String view) throws IOException
   {
/* 223 */     invokeMethod(client, viewRoot, view, "invalidate", new Object[0]);
   }
 
   public static void requestLayout(Client client, String viewRoot, String view) throws IOException
   {
/* 228 */     invokeMethod(client, viewRoot, view, "requestLayout", new Object[0]);
   }
 
   public static void dumpDisplayList(Client client, String viewRoot, String view) throws IOException
   {
/* 233 */     sendViewOpPacket(client, 2, viewRoot, view, null, sViewOpNullChunkHandler);
   }
 
   public static void dumpTheme(Client client, String viewRoot, ViewDumpHandler handler)
     throws IOException
   {
/* 240 */     ByteBuffer buf = allocBuffer(8 + viewRoot.length() * 2);
 
/* 243 */     JdwpPacket packet = new JdwpPacket(buf);
/* 244 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
 
/* 246 */     chunkBuf.putInt(3);
/* 247 */     chunkBuf.putInt(viewRoot.length());
/* 248 */     ByteBufferUtil.putString(chunkBuf, viewRoot);
 
/* 250 */     finishChunkPacket(packet, CHUNK_VURT, chunkBuf.position());
/* 251 */     client.sendAndConsume(packet, handler);
   }
 
   public static void invokeMethod(Client client, String viewRoot, String view, String method, Object[] args)
     throws IOException
   {
/* 267 */     int len = 4 + method.length() * 2;
/* 268 */     if (args != null)
     {
/* 270 */       len += 4;
 
/* 274 */       len += 10 * args.length;
     }
 
/* 277 */     byte[] extra = new byte[len];
/* 278 */     ByteBuffer b = ByteBuffer.wrap(extra);
 
/* 280 */     b.putInt(method.length());
/* 281 */     ByteBufferUtil.putString(b, method);
 
/* 283 */     if (args != null) {
/* 284 */       b.putInt(args.length);
 
/* 286 */       for (int i = 0; i < args.length; i++) {
/* 287 */         Object arg = args[i];
/* 288 */         if ((arg instanceof Boolean)) {
/* 289 */           b.putChar('Z');
/* 290 */           b.put((byte)(((Boolean)arg).booleanValue() ? 1 : 0));
/* 291 */         } else if ((arg instanceof Byte)) {
/* 292 */           b.putChar('B');
/* 293 */           b.put(((Byte)arg).byteValue());
/* 294 */         } else if ((arg instanceof Character)) {
/* 295 */           b.putChar('C');
/* 296 */           b.putChar(((Character)arg).charValue());
/* 297 */         } else if ((arg instanceof Short)) {
/* 298 */           b.putChar('S');
/* 299 */           b.putShort(((Short)arg).shortValue());
/* 300 */         } else if ((arg instanceof Integer)) {
/* 301 */           b.putChar('I');
/* 302 */           b.putInt(((Integer)arg).intValue());
/* 303 */         } else if ((arg instanceof Long)) {
/* 304 */           b.putChar('J');
/* 305 */           b.putLong(((Long)arg).longValue());
/* 306 */         } else if ((arg instanceof Float)) {
/* 307 */           b.putChar('F');
/* 308 */           b.putFloat(((Float)arg).floatValue());
/* 309 */         } else if ((arg instanceof Double)) {
/* 310 */           b.putChar('D');
/* 311 */           b.putDouble(((Double)arg).doubleValue());
         } else {
/* 313 */           Log.e("ddmlib", "View method invocation only supports primitive arguments, supplied: " + arg);
/* 314 */           return;
         }
       }
     }
 
/* 319 */     sendViewOpPacket(client, 4, viewRoot, view, extra, sViewOpNullChunkHandler);
   }
 
   public static void setLayoutParameter(Client client, String viewRoot, String view, String parameter, int value)
     throws IOException
   {
/* 325 */     int len = 4 + parameter.length() * 2 + 4;
/* 326 */     byte[] extra = new byte[len];
/* 327 */     ByteBuffer b = ByteBuffer.wrap(extra);
 
/* 329 */     b.putInt(parameter.length());
/* 330 */     ByteBufferUtil.putString(b, parameter);
/* 331 */     b.putInt(value);
/* 332 */     sendViewOpPacket(client, 5, viewRoot, view, extra, sViewOpNullChunkHandler);
   }
 
   public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
   {
   }
 
   public static void sendStartGlTracing(Client client)
     throws IOException
   {
/* 342 */     ByteBuffer buf = allocBuffer(4);
/* 343 */     JdwpPacket packet = new JdwpPacket(buf);
 
/* 345 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
/* 346 */     chunkBuf.putInt(1);
/* 347 */     finishChunkPacket(packet, CHUNK_VUGL, chunkBuf.position());
 
/* 349 */     client.sendAndConsume(packet);
   }
 
   public static void sendStopGlTracing(Client client) throws IOException {
/* 353 */     ByteBuffer buf = allocBuffer(4);
/* 354 */     JdwpPacket packet = new JdwpPacket(buf);
 
/* 356 */     ByteBuffer chunkBuf = getChunkDataBuf(buf);
/* 357 */     chunkBuf.putInt(0);
/* 358 */     finishChunkPacket(packet, CHUNK_VUGL, chunkBuf.position());
 
/* 360 */     client.sendAndConsume(packet);
   }
 
   private static class NullChunkHandler extends HandleViewDebug.ViewDumpHandler
   {
     public NullChunkHandler(int chunkType)
     {
/* 257 */       super(chunkType);
     }
 
     protected void handleViewDebugResult(ByteBuffer data)
     {
     }
   }
 
   public static abstract class ViewDumpHandler extends ChunkHandler
   {
/*  92 */     private final CountDownLatch mLatch = new CountDownLatch(1);
     private final int mChunkType;
 
     public ViewDumpHandler(int chunkType)
     {
/*  96 */       this.mChunkType = chunkType;
     }
 
     void clientReady(Client client)
       throws IOException
     {
     }
 
     void clientDisconnected(Client client)
     {
     }
 
     void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId)
     {
/* 110 */       if (type != this.mChunkType) {
/* 111 */         handleUnknownChunk(client, type, data, isReply, msgId);
/* 112 */         return;
       }
 
/* 115 */       handleViewDebugResult(data);
/* 116 */       this.mLatch.countDown();
     }
 
     protected abstract void handleViewDebugResult(ByteBuffer paramByteBuffer);
 
     protected void waitForResult(long timeout, TimeUnit unit) {
       try {
/* 123 */         this.mLatch.await(timeout, unit);
       }
       catch (InterruptedException e)
       {
       }
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleViewDebug
 * JD-Core Version:    0.6.2
 */