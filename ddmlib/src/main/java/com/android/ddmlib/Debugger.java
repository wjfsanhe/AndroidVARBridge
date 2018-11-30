 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.ServerSocket;
 import java.nio.BufferOverflowException;
 import java.nio.ByteBuffer;
 import java.nio.channels.Selector;
 import java.nio.channels.ServerSocketChannel;
 import java.nio.channels.SocketChannel;
 
 class Debugger
 {
   private static final int INITIAL_BUF_SIZE = 1024;
   private static final int MAX_BUF_SIZE = 32768;
   private ByteBuffer mReadBuffer;
   private static final int PRE_DATA_BUF_SIZE = 256;
   private ByteBuffer mPreDataBuffer;
   private int mConnState;
   private static final int ST_NOT_CONNECTED = 1;
   private static final int ST_AWAIT_SHAKE = 2;
   private static final int ST_READY = 3;
   private Client mClient;
   private int mListenPort;
   private ServerSocketChannel mListenChannel;
   private SocketChannel mChannel;
 
   Debugger(Client client, int listenPort)
     throws IOException
   {
/*  67 */     this.mClient = client;
/*  68 */     this.mListenPort = listenPort;
 
/*  70 */     this.mListenChannel = ServerSocketChannel.open();
/*  71 */     this.mListenChannel.configureBlocking(false);
 
/*  73 */     InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), listenPort);
 
/*  76 */     this.mListenChannel.socket().setReuseAddress(true);
/*  77 */     this.mListenChannel.socket().bind(addr);
 
/*  79 */     this.mReadBuffer = ByteBuffer.allocate(1024);
/*  80 */     this.mPreDataBuffer = ByteBuffer.allocate(256);
/*  81 */     this.mConnState = 1;
 
/*  83 */     Log.d("ddms", new StringBuilder().append("Created: ").append(toString()).toString());
   }
 
   boolean isDebuggerAttached()
   {
/*  90 */     return this.mChannel != null;
   }
 
   public String toString()
   {
/*  99 */     return new StringBuilder().append("[Debugger ").append(this.mListenPort).append("-->").append(this.mClient.getClientData().getPid()).append(this.mConnState != 3 ? " inactive]" : " active]").toString();
   }
 
   void registerListener(Selector sel)
     throws IOException
   {
/* 107 */     this.mListenChannel.register(sel, 16, this);
   }
 
   Client getClient()
   {
/* 114 */     return this.mClient;
   }
 
   synchronized SocketChannel accept()
     throws IOException
   {
/* 125 */     return accept(this.mListenChannel);
   }
 
   synchronized SocketChannel accept(ServerSocketChannel listenChan)
     throws IOException
   {
/* 140 */     if (listenChan != null)
     {
/* 143 */       SocketChannel newChan = listenChan.accept();
/* 144 */       if (this.mChannel != null) {
/* 145 */         Log.w("ddms", new StringBuilder().append("debugger already talking to ").append(this.mClient).append(" on ").append(this.mListenPort).toString());
 
/* 147 */         newChan.close();
/* 148 */         return null;
       }
/* 150 */       this.mChannel = newChan;
/* 151 */       this.mChannel.configureBlocking(false);
/* 152 */       this.mConnState = 2;
/* 153 */       return this.mChannel;
     }
 
/* 156 */     return null;
   }
 
   synchronized void closeData()
   {
     try
     {
/* 164 */       if (this.mChannel != null) {
/* 165 */         this.mChannel.close();
/* 166 */         this.mChannel = null;
/* 167 */         this.mConnState = 1;
 
/* 169 */         ClientData cd = this.mClient.getClientData();
/* 170 */         cd.setDebuggerConnectionStatus(ClientData.DebuggerStatus.DEFAULT);
/* 171 */         this.mClient.update(2);
       }
     } catch (IOException ioe) {
/* 174 */       Log.w("ddms", new StringBuilder().append("Failed to close data ").append(this).toString());
     }
   }
 
   synchronized void close()
   {
     try
     {
/* 184 */       if (this.mListenChannel != null) {
/* 185 */         this.mListenChannel.close();
       }
/* 187 */       this.mListenChannel = null;
/* 188 */       closeData();
     } catch (IOException ioe) {
/* 190 */       Log.w("ddms", new StringBuilder().append("Failed to close listener ").append(this).toString());
     }
   }
 
   void read()
     throws IOException
   {
/* 206 */     if (this.mReadBuffer.position() == this.mReadBuffer.capacity()) {
/* 207 */       if (this.mReadBuffer.capacity() * 2 > 32768) {
/* 208 */         throw new BufferOverflowException();
       }
/* 210 */       Log.d("ddms", new StringBuilder().append("Expanding read buffer to ").append(this.mReadBuffer.capacity() * 2).toString());
 
/* 213 */       ByteBuffer newBuffer = ByteBuffer.allocate(this.mReadBuffer.capacity() * 2);
 
/* 215 */       this.mReadBuffer.position(0);
/* 216 */       newBuffer.put(this.mReadBuffer);
 
/* 218 */       this.mReadBuffer = newBuffer;
     }
 
/* 221 */     int count = this.mChannel.read(this.mReadBuffer);
/* 222 */     Log.v("ddms", new StringBuilder().append("Read ").append(count).append(" bytes from ").append(this).toString());
/* 223 */     if (count < 0) throw new IOException("read failed");
   }
 
   JdwpPacket getJdwpPacket()
     throws IOException
   {
/* 241 */     if (this.mConnState == 2)
     {
/* 244 */       int result = JdwpPacket.findHandshake(this.mReadBuffer);
 
/* 246 */       switch (result) {
       case 1:
/* 248 */         Log.d("ddms", "Good handshake from debugger");
/* 249 */         JdwpPacket.consumeHandshake(this.mReadBuffer);
/* 250 */         sendHandshake();
/* 251 */         this.mConnState = 3;
 
/* 253 */         ClientData cd = this.mClient.getClientData();
/* 254 */         cd.setDebuggerConnectionStatus(ClientData.DebuggerStatus.ATTACHED);
/* 255 */         this.mClient.update(2);
 
/* 258 */         return getJdwpPacket();
       case 3:
/* 261 */         Log.d("ddms", "Bad handshake from debugger");
/* 262 */         throw new IOException("bad handshake");
       case 2:
/* 264 */         break;
       default:
/* 266 */         Log.e("ddms", "Unknown packet while waiting for client handshake");
       }
/* 268 */       return null;
/* 269 */     }if (this.mConnState == 3) {
/* 270 */       if (this.mReadBuffer.position() != 0) {
/* 271 */         Log.v("ddms", new StringBuilder().append("Checking ").append(this.mReadBuffer.position()).append(" bytes").toString());
       }
/* 273 */       return JdwpPacket.findPacket(this.mReadBuffer);
     }
/* 275 */     Log.e("ddms", new StringBuilder().append("Receiving data in state = ").append(this.mConnState).toString());
 
/* 278 */     return null;
   }
 
   void forwardPacketToClient(JdwpPacket packet)
     throws IOException
   {
/* 290 */     this.mClient.sendAndConsume(packet);
   }
 
   private synchronized void sendHandshake()
     throws IOException
   {
/* 299 */     ByteBuffer tempBuffer = ByteBuffer.allocate(JdwpPacket.HANDSHAKE_LEN);
/* 300 */     JdwpPacket.putHandshake(tempBuffer);
/* 301 */     int expectedLength = tempBuffer.position();
/* 302 */     tempBuffer.flip();
/* 303 */     if (this.mChannel.write(tempBuffer) != expectedLength) {
/* 304 */       throw new IOException("partial handshake write");
     }
 
/* 307 */     expectedLength = this.mPreDataBuffer.position();
/* 308 */     if (expectedLength > 0) {
/* 309 */       Log.d("ddms", new StringBuilder().append("Sending ").append(this.mPreDataBuffer.position()).append(" bytes of saved data").toString());
 
/* 311 */       this.mPreDataBuffer.flip();
/* 312 */       if (this.mChannel.write(this.mPreDataBuffer) != expectedLength) {
/* 313 */         throw new IOException("partial pre-data write");
       }
/* 315 */       this.mPreDataBuffer.clear();
     }
   }
 
   synchronized void sendAndConsume(JdwpPacket packet)
     throws IOException
   {
/* 336 */     if (this.mChannel == null)
     {
/* 345 */       Log.d("ddms", new StringBuilder().append("Saving packet 0x").append(Integer.toHexString(packet.getId())).toString());
 
/* 347 */       packet.movePacket(this.mPreDataBuffer);
     } else {
/* 349 */       packet.writeAndConsume(this.mChannel);
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.Debugger
 * JD-Core Version:    0.6.2
 */