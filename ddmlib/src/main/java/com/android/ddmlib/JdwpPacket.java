 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.channels.SocketChannel;
 
 final class JdwpPacket
 {
   public static final int JDWP_HEADER_LEN = 11;
   public static final int HANDSHAKE_GOOD = 1;
   public static final int HANDSHAKE_NOTYET = 2;
   public static final int HANDSHAKE_BAD = 3;
   private static final int DDMS_CMD_SET = 199;
   private static final int DDMS_CMD = 1;
   private static final int REPLY_PACKET = 128;
/*  54 */   private static final byte[] mHandshake = { 74, 68, 87, 80, 45, 72, 97, 110, 100, 115, 104, 97, 107, 101 };
 
/*  58 */   public static final int HANDSHAKE_LEN = mHandshake.length;
   private ByteBuffer mBuffer;
   private int mLength;
   private int mId;
   private int mFlags;
   private int mCmdSet;
   private int mCmd;
   private int mErrCode;
   private boolean mIsNew;
/*  64 */   private static int sSerialId = 1073741824;
 
   JdwpPacket(ByteBuffer buf)
   {
/*  71 */     this.mBuffer = buf;
/*  72 */     this.mIsNew = true;
   }
 
   void finishPacket(int payloadLength)
   {
/*  89 */     assert (this.mIsNew);
 
/*  91 */     ByteOrder oldOrder = this.mBuffer.order();
/*  92 */     this.mBuffer.order(ChunkHandler.CHUNK_ORDER);
 
/*  94 */     this.mLength = (11 + payloadLength);
/*  95 */     this.mId = getNextSerial();
/*  96 */     this.mFlags = 0;
/*  97 */     this.mCmdSet = 199;
/*  98 */     this.mCmd = 1;
 
/* 100 */     this.mBuffer.putInt(0, this.mLength);
/* 101 */     this.mBuffer.putInt(4, this.mId);
/* 102 */     this.mBuffer.put(8, (byte)this.mFlags);
/* 103 */     this.mBuffer.put(9, (byte)this.mCmdSet);
/* 104 */     this.mBuffer.put(10, (byte)this.mCmd);
 
/* 106 */     this.mBuffer.order(oldOrder);
/* 107 */     this.mBuffer.position(this.mLength);
   }
 
   private static synchronized int getNextSerial()
   {
/* 118 */     return sSerialId++;
   }
 
   ByteBuffer getPayload()
   {
/* 132 */     int oldPosn = this.mBuffer.position();
 
/* 134 */     this.mBuffer.position(11);
/* 135 */     ByteBuffer buf = this.mBuffer.slice();
/* 136 */     this.mBuffer.position(oldPosn);
 
/* 138 */     if (this.mLength > 0)
/* 139 */       buf.limit(this.mLength - 11);
     else
/* 141 */       assert (this.mIsNew);
/* 142 */     buf.order(ChunkHandler.CHUNK_ORDER);
/* 143 */     return buf;
   }
 
   boolean isDdmPacket()
   {
/* 152 */     return ((this.mFlags & 0x80) == 0) && (this.mCmdSet == 199) && (this.mCmd == 1);
   }
 
   boolean isReply()
   {
/* 161 */     return (this.mFlags & 0x80) != 0;
   }
 
   boolean isError()
   {
/* 169 */     return (isReply()) && (this.mErrCode != 0);
   }
 
   boolean isEmpty()
   {
/* 176 */     return this.mLength == 11;
   }
 
   int getId()
   {
/* 184 */     return this.mId;
   }
 
   int getLength()
   {
/* 192 */     return this.mLength;
   }
 
   void writeAndConsume(SocketChannel chan)
     throws IOException
   {
/* 207 */     assert (this.mLength > 0);
 
/* 209 */     this.mBuffer.flip();
/* 210 */     int oldLimit = this.mBuffer.limit();
/* 211 */     this.mBuffer.limit(this.mLength);
/* 212 */     while (this.mBuffer.position() != this.mBuffer.limit()) {
/* 213 */       chan.write(this.mBuffer);
     }
 
/* 216 */     assert (this.mBuffer.position() == this.mLength);
 
/* 218 */     this.mBuffer.limit(oldLimit);
/* 219 */     this.mBuffer.compact();
   }
 
   void movePacket(ByteBuffer buf)
   {
/* 230 */     Log.v("ddms", "moving " + this.mLength + " bytes");
/* 231 */     int oldPosn = this.mBuffer.position();
 
/* 233 */     this.mBuffer.position(0);
/* 234 */     this.mBuffer.limit(this.mLength);
/* 235 */     buf.put(this.mBuffer);
/* 236 */     this.mBuffer.position(this.mLength);
/* 237 */     this.mBuffer.limit(oldPosn);
/* 238 */     this.mBuffer.compact();
   }
 
   void consume()
   {
/* 265 */     this.mBuffer.flip();
/* 266 */     this.mBuffer.position(this.mLength);
/* 267 */     this.mBuffer.compact();
/* 268 */     this.mLength = 0;
   }
 
   static JdwpPacket findPacket(ByteBuffer buf)
   {
/* 286 */     int count = buf.position();
 
/* 289 */     if (count < 11) {
/* 290 */       return null;
     }
/* 292 */     ByteOrder oldOrder = buf.order();
/* 293 */     buf.order(ChunkHandler.CHUNK_ORDER);
 
/* 295 */     int length = buf.getInt(0);
/* 296 */     int id = buf.getInt(4);
/* 297 */     int flags = buf.get(8) & 0xFF;
/* 298 */     int cmdSet = buf.get(9) & 0xFF;
/* 299 */     int cmd = buf.get(10) & 0xFF;
 
/* 301 */     buf.order(oldOrder);
 
/* 303 */     if (length < 11)
/* 304 */       throw new BadPacketException();
/* 305 */     if (count < length) {
/* 306 */       return null;
     }
/* 308 */     JdwpPacket pkt = new JdwpPacket(buf);
 
/* 310 */     pkt.mLength = length;
/* 311 */     pkt.mId = id;
/* 312 */     pkt.mFlags = flags;
 
/* 314 */     if ((flags & 0x80) == 0) {
/* 315 */       pkt.mCmdSet = cmdSet;
/* 316 */       pkt.mCmd = cmd;
/* 317 */       pkt.mErrCode = -1;
     } else {
/* 319 */       pkt.mCmdSet = -1;
/* 320 */       pkt.mCmd = -1;
/* 321 */       pkt.mErrCode = (cmdSet | cmd << 8);
     }
 
/* 324 */     return pkt;
   }
 
   static int findHandshake(ByteBuffer buf)
   {
/* 336 */     int count = buf.position();
 
/* 339 */     if (count < mHandshake.length) {
/* 340 */       return 2;
     }
/* 342 */     for (int i = mHandshake.length - 1; i >= 0; i--) {
/* 343 */       if (buf.get(i) != mHandshake[i]) {
/* 344 */         return 3;
       }
     }
/* 347 */     return 1;
   }
 
   static void consumeHandshake(ByteBuffer buf)
   {
/* 357 */     buf.flip();
/* 358 */     buf.position(mHandshake.length);
/* 359 */     buf.compact();
   }
 
   static void putHandshake(ByteBuffer buf)
   {
/* 368 */     buf.put(mHandshake);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.JdwpPacket
 * JD-Core Version:    0.6.2
 */