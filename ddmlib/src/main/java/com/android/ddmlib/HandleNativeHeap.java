 package com.android.ddmlib;
 
 import java.io.BufferedReader;
 import java.io.ByteArrayInputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 
 final class HandleNativeHeap extends ChunkHandler
 {
/*  31 */   public static final int CHUNK_NHGT = type("NHGT");
/*  32 */   public static final int CHUNK_NHSG = type("NHSG");
/*  33 */   public static final int CHUNK_NHST = type("NHST");
/*  34 */   public static final int CHUNK_NHEN = type("NHEN");
 
/*  36 */   private static final HandleNativeHeap mInst = new HandleNativeHeap();
 
   public static void register(MonitorThread mt)
   {
/*  96 */     mt.registerChunkHandler(CHUNK_NHGT, mInst);
/*  97 */     mt.registerChunkHandler(CHUNK_NHSG, mInst);
/*  98 */     mt.registerChunkHandler(CHUNK_NHST, mInst);
/*  99 */     mt.registerChunkHandler(CHUNK_NHEN, mInst);
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
/* 120 */     Log.d("ddm-nativeheap", "handling " + ChunkHandler.name(type));
 
/* 122 */     if (type == CHUNK_NHGT)
/* 123 */       handleNHGT(client, data);
/* 124 */     else if (type == CHUNK_NHST)
     {
/* 126 */       client.getClientData().getNativeHeapData().clearHeapData();
/* 127 */     } else if (type == CHUNK_NHEN)
     {
/* 129 */       client.getClientData().getNativeHeapData().sealHeapData();
/* 130 */     } else if (type == CHUNK_NHSG)
/* 131 */       handleNHSG(client, data);
     else {
/* 133 */       handleUnknownChunk(client, type, data, isReply, msgId);
     }
 
/* 136 */     client.update(128);
   }
 
   public static void sendNHGT(Client client)
     throws IOException
   {
/* 144 */     ByteBuffer rawBuf = allocBuffer(0);
/* 145 */     JdwpPacket packet = new JdwpPacket(rawBuf);
/* 146 */     ByteBuffer buf = getChunkDataBuf(rawBuf);
 
/* 150 */     finishChunkPacket(packet, CHUNK_NHGT, buf.position());
/* 151 */     Log.d("ddm-nativeheap", "Sending " + name(CHUNK_NHGT));
/* 152 */     client.sendAndConsume(packet, mInst);
 
/* 154 */     rawBuf = allocBuffer(2);
/* 155 */     packet = new JdwpPacket(rawBuf);
/* 156 */     buf = getChunkDataBuf(rawBuf);
 
/* 158 */     buf.put((byte)0);
/* 159 */     buf.put((byte)1);
 
/* 161 */     finishChunkPacket(packet, CHUNK_NHSG, buf.position());
/* 162 */     Log.d("ddm-nativeheap", "Sending " + name(CHUNK_NHSG));
/* 163 */     client.sendAndConsume(packet, mInst);
   }
 
   private void handleNHGT(Client client, ByteBuffer data)
   {
/* 170 */     ClientData clientData = client.getClientData();
 
/* 172 */     Log.d("ddm-nativeheap", "NHGT: " + data.limit() + " bytes");
 
/* 174 */     data.order(ByteOrder.LITTLE_ENDIAN);
 
/* 201 */     int signature = data.getInt(0);
/* 202 */     short pointerSize = 4;
/* 203 */     if (signature == -2128394787)
     {
/* 205 */       int ignore = data.getInt();
/* 206 */       short version = data.getShort();
/* 207 */       if (version != 2) {
/* 208 */         Log.e("ddms", "Unknown header version: " + version);
/* 209 */         return;
       }
/* 211 */       pointerSize = data.getShort();
     }
     NativeBuffer buffer;
/* 214 */     if (pointerSize == 4) {
/* 215 */       buffer = new NativeBuffer32(data);
     }
     else
     {

/* 216 */       if (pointerSize == 8) {
/* 217 */         buffer = new NativeBuffer64(data);
       } else {
/* 219 */         Log.e("ddms", "Unknown pointer size: " + pointerSize);
         return;
       }
     }

/* 224 */     clientData.clearNativeAllocationInfo();
 
/* 226 */     int mapSize = buffer.getSizeT();
/* 227 */     int allocSize = buffer.getSizeT();
/* 228 */     int allocInfoSize = buffer.getSizeT();
/* 229 */     int totalMemory = buffer.getSizeT();
/* 230 */     int backtraceSize = buffer.getSizeT();
 
/* 232 */     Log.d("ddms", "mapSize: " + mapSize);
/* 233 */     Log.d("ddms", "allocSize: " + allocSize);
/* 234 */     Log.d("ddms", "allocInfoSize: " + allocInfoSize);
/* 235 */     Log.d("ddms", "totalMemory: " + totalMemory);
 
/* 237 */     clientData.setTotalNativeMemory(totalMemory);
 
/* 240 */     if (allocInfoSize == 0) {
/* 241 */       return;
     }
 
/* 244 */     if (mapSize > 0) {
/* 245 */       byte[] maps = new byte[mapSize];
/* 246 */       data.get(maps, 0, mapSize);
/* 247 */       parseMaps(clientData, maps);
     }
 
/* 250 */     int iterations = allocSize / allocInfoSize;
/* 251 */     for (int i = 0; i < iterations; i++) {
/* 252 */       NativeAllocationInfo info = new NativeAllocationInfo(buffer.getSizeT(), buffer.getSizeT());
 
/* 256 */       for (int j = 0; j < backtraceSize; j++) {
/* 257 */         long addr = buffer.getPtr();
/* 258 */         if (addr != 0L)
         {
/* 263 */           info.addStackCallAddress(addr);
         }
       }
/* 265 */       clientData.addNativeAllocation(info);
     }
   }
 
   private void handleNHSG(Client client, ByteBuffer data) {
/* 270 */     byte[] dataCopy = new byte[data.limit()];
/* 271 */     data.rewind();
/* 272 */     data.get(dataCopy);
/* 273 */     data = ByteBuffer.wrap(dataCopy);
/* 274 */     client.getClientData().getNativeHeapData().addHeapData(data);
   }
 
   private void parseMaps(ClientData clientData, byte[] maps)
   {
/* 300 */     InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(maps));
/* 301 */     BufferedReader reader = new BufferedReader(input);
     try
     {
       String line;
/* 306 */       while ((line = reader.readLine()) != null) {
/* 307 */         Log.d("ddms", "line: " + line);
 
/* 311 */         int library_start = line.lastIndexOf(' ');
/* 312 */         if (library_start != -1)
         {
/* 318 */           String library = line.substring(library_start + 1);
/* 319 */           if (library.startsWith("/"))
           {
/* 324 */             int dashIndex = line.indexOf('-');
/* 325 */             int spaceIndex = line.indexOf(' ', dashIndex);
/* 326 */             if ((dashIndex != -1) && (spaceIndex != -1))
             {
/* 330 */               long startAddr = 0L;
/* 331 */               long endAddr = 0L;
               try {
/* 333 */                 startAddr = Long.parseLong(line.substring(0, dashIndex), 16);
/* 334 */                 endAddr = Long.parseLong(line.substring(dashIndex + 1, spaceIndex), 16);
               } catch (NumberFormatException e) {
/* 336 */                 e.printStackTrace();
/* 337 */               }
 
/* 340 */               clientData.addNativeLibraryMapInfo(startAddr, endAddr, library);
/* 341 */               Log.d("ddms", library + "(" + Long.toHexString(startAddr) + " - " + Long.toHexString(endAddr) + ")");
             }
           }
         }
       } } catch (IOException e) { e.printStackTrace(); }
 
   }
 
   final class NativeBuffer64 extends HandleNativeHeap.NativeBuffer
   {
     public NativeBuffer64(ByteBuffer buffer)
     {
/*  75 */       super(buffer);
     }
 
     public int getSizeT()
     {
/*  80 */       return (int)this.mBuffer.getLong();
     }
 
     public long getPtr() {
/*  84 */       return this.mBuffer.getLong();
     }
   }
 
   final class NativeBuffer32 extends HandleNativeHeap.NativeBuffer
   {
     public NativeBuffer32(ByteBuffer buffer)
     {
/*  57 */       super(buffer);
     }
 
     public int getSizeT()
     {
/*  62 */       return this.mBuffer.getInt();
     }
 
     public long getPtr() {
/*  66 */       return this.mBuffer.getInt() & 0xFFFFFFFF;
     }
   }
 
   abstract class NativeBuffer
   {
     protected ByteBuffer mBuffer;
 
     public NativeBuffer(ByteBuffer buffer)
     {
/*  43 */       this.mBuffer = buffer;
     }
 
     public abstract int getSizeT();
 
     public abstract long getPtr();
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.HandleNativeHeap
 * JD-Core Version:    0.6.2
 */