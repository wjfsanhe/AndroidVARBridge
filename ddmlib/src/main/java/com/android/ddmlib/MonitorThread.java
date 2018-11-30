 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.net.BindException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.net.ServerSocket;
 import java.net.Socket;
 import java.nio.BufferOverflowException;
 import java.nio.ByteBuffer;
 import java.nio.channels.CancelledKeyException;
 import java.nio.channels.NotYetBoundException;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.nio.channels.ServerSocketChannel;
 import java.nio.channels.SocketChannel;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.Set;
 
 final class MonitorThread extends Thread
 {
   private static final int CLIENT_READY = 2;
   private static final int CLIENT_DISCONNECTED = 3;
/*  53 */   private volatile boolean mQuit = false;
   private final ArrayList<Client> mClientList;
   private Selector mSelector;
   private final HashMap<Integer, ChunkHandler> mHandlerMap;
   private ServerSocketChannel mDebugSelectedChan;
   private int mNewDebugSelectedPort;
/*  71 */   private int mDebugSelectedPort = -1;
 
/*  76 */   private Client mSelectedClient = null;
   private static MonitorThread sInstance;
 
   private MonitorThread()
   {
/*  85 */     super("Monitor");
/*  86 */     this.mClientList = new ArrayList();
/*  87 */     this.mHandlerMap = new HashMap();
 
/*  89 */     this.mNewDebugSelectedPort = DdmPreferences.getSelectedDebugPort();
   }
 
   static MonitorThread createInstance()
   {
/*  96 */     return MonitorThread.sInstance = new MonitorThread();
   }
 
   static MonitorThread getInstance()
   {
/* 103 */     return sInstance;
   }
 
   synchronized void setDebugSelectedPort(int port)
     throws IllegalStateException
   {
/* 111 */     if (sInstance == null) {
/* 112 */       return;
     }
 
/* 115 */     if (!AndroidDebugBridge.getClientSupport()) {
/* 116 */       return;
     }
 
/* 119 */     if (this.mDebugSelectedChan != null) {
/* 120 */       Log.d("ddms", new StringBuilder().append("Changing debug-selected port to ").append(port).toString());
/* 121 */       this.mNewDebugSelectedPort = port;
/* 122 */       wakeup();
     }
     else
     {
/* 126 */       this.mNewDebugSelectedPort = port;
     }
   }
 
   synchronized void setSelectedClient(Client selectedClient)
   {
/* 135 */     if (sInstance == null) {
/* 136 */       return;
     }
 
/* 139 */     if (this.mSelectedClient != selectedClient) {
/* 140 */       Client oldClient = this.mSelectedClient;
/* 141 */       this.mSelectedClient = selectedClient;
 
/* 143 */       if (oldClient != null) {
/* 144 */         oldClient.update(4);
       }
 
/* 147 */       if (this.mSelectedClient != null)
/* 148 */         this.mSelectedClient.update(4);
     }
   }
 
   Client getSelectedClient()
   {
/* 157 */     return this.mSelectedClient;
   }
 
   boolean getRetryOnBadHandshake()
   {
/* 167 */     return true;
   }
 
   Client[] getClients()
   {
/* 174 */     synchronized (this.mClientList) {
/* 175 */       return (Client[])this.mClientList.toArray(new Client[this.mClientList.size()]);
     }
   }
 
   synchronized void registerChunkHandler(int type, ChunkHandler handler)
   {
/* 183 */     if (sInstance == null) {
/* 184 */       return;
     }
 
/* 187 */     synchronized (this.mHandlerMap) {
/* 188 */       if (this.mHandlerMap.get(Integer.valueOf(type)) == null)
/* 189 */         this.mHandlerMap.put(Integer.valueOf(type), handler);
     }
   }
 
   public void run()
   {
/* 199 */     Log.d("ddms", "Monitor is up");
     try
     {
/* 203 */       this.mSelector = Selector.open();
     } catch (IOException ioe) {
/* 205 */       Log.logAndDisplay(Log.LogLevel.ERROR, "ddms", new StringBuilder().append("Failed to initialize Monitor Thread: ").append(ioe.getMessage()).toString());
 
/* 207 */       return;
     }
 
/* 210 */     while (!this.mQuit)
     {
       try
       {
/* 218 */         synchronized (this.mClientList)
         {
         }
 
         try
         {
/* 224 */           if ((AndroidDebugBridge.getClientSupport()) && 
/* 225 */             ((this.mDebugSelectedChan == null) || (this.mNewDebugSelectedPort != this.mDebugSelectedPort)) && (this.mNewDebugSelectedPort != -1))
           {
/* 228 */             if (reopenDebugSelectedPort())
/* 229 */               this.mDebugSelectedPort = this.mNewDebugSelectedPort;
           }
         }
         catch (IOException ioe)
         {
/* 234 */           Log.e("ddms", new StringBuilder().append("Failed to reopen debug port for Selected Client to: ").append(this.mNewDebugSelectedPort).toString());
 
/* 236 */           Log.e("ddms", ioe);
/* 237 */           this.mNewDebugSelectedPort = this.mDebugSelectedPort;
         }
         int count;
         try
         {
/* 242 */           count = this.mSelector.select();
         } catch (IOException ioe) {
/* 244 */           ioe.printStackTrace();
/* 245 */           continue; }
 
/* 250 */         if (count != 0)
         {
/* 256 */           Set keys = this.mSelector.selectedKeys();
/* 257 */           Iterator iter = keys.iterator();
 
/* 259 */           while (iter.hasNext()) {
/* 260 */             SelectionKey key = (SelectionKey)iter.next();
/* 261 */             iter.remove();
             try
             {
/* 264 */               if ((key.attachment() instanceof Client)) {
/* 265 */                 processClientActivity(key);
               }
/* 267 */               else if ((key.attachment() instanceof Debugger)) {
/* 268 */                 processDebuggerActivity(key);
               }
/* 270 */               else if ((key.attachment() instanceof MonitorThread)) {
/* 271 */                 processDebugSelectedActivity(key);
               }
               else {
/* 274 */                 Log.e("ddms", "unknown activity key");
               }
             }
             catch (Exception e)
             {
/* 279 */               Log.e("ddms", "Exception during activity from Selector.");
/* 280 */               Log.e("ddms", e);
             }
           }
         }
       }
       catch (Exception e) {
/* 286 */         Log.e("ddms", "Exception MonitorThread.run()");
/* 287 */         Log.e("ddms", e);
       }
     }
   }
 
   int getDebugSelectedPort()
   {
/* 297 */     return this.mDebugSelectedPort;
   }
 
   private void processClientActivity(SelectionKey key)
   {
/* 304 */     Client client = (Client)key.attachment();
     try
     {
/* 307 */       if ((!key.isReadable()) || (!key.isValid())) {
/* 308 */         Log.d("ddms", new StringBuilder().append("Invalid key from ").append(client).append(". Dropping client.").toString());
/* 309 */         dropClient(client, true);
/* 310 */         return;
       }
 
/* 313 */       client.read();
 
/* 319 */       JdwpPacket packet = client.getJdwpPacket();
/* 320 */       while (packet != null) {
/* 321 */         if (packet.isDdmPacket())
         {
/* 323 */           assert (!packet.isReply());
/* 324 */           callHandler(client, packet, null);
/* 325 */           packet.consume();
/* 326 */         } else if ((packet.isReply()) && (client.isResponseToUs(packet.getId()) != null))
         {
/* 329 */           ChunkHandler handler = client.isResponseToUs(packet.getId());
 
/* 331 */           if (packet.isError())
/* 332 */             client.packetFailed(packet);
/* 333 */           else if (packet.isEmpty()) {
/* 334 */             Log.d("ddms", new StringBuilder().append("Got empty reply for 0x").append(Integer.toHexString(packet.getId())).append(" from ").append(client).toString());
           }
           else
           {
/* 338 */             callHandler(client, packet, handler);
/* 339 */           }packet.consume();
/* 340 */           client.removeRequestId(packet.getId());
         } else {
/* 342 */           Log.v("ddms", new StringBuilder().append("Forwarding client ").append(packet.isReply() ? "reply" : "event").append(" 0x").append(Integer.toHexString(packet.getId())).append(" to ").append(client.getDebugger()).toString());
 
/* 346 */           client.forwardPacketToDebugger(packet);
         }
 
/* 350 */         packet = client.getJdwpPacket();
       }
     }
     catch (CancelledKeyException e)
     {
/* 355 */       dropClient(client, true);
     }
     catch (IOException ex) {
/* 358 */       dropClient(client, true);
     } catch (Exception ex) {
/* 360 */       Log.e("ddms", ex);
 
/* 363 */       dropClient(client, true);
 
/* 365 */       if ((ex instanceof BufferOverflowException)) {
/* 366 */         Log.w("ddms", new StringBuilder().append("Client data packet exceeded maximum buffer size ").append(client).toString());
       }
       else
       {
/* 371 */         Log.e("ddms", ex);
       }
     }
   }
 
   private void callHandler(Client client, JdwpPacket packet, ChunkHandler handler)
   {
/* 385 */     if (!client.ddmSeen()) {
/* 386 */       broadcast(2, client);
     }
/* 388 */     ByteBuffer buf = packet.getPayload();
 
/* 390 */     boolean reply = true;
 
/* 392 */     int type = buf.getInt();
/* 393 */     int length = buf.getInt();
 
/* 395 */     if (handler == null)
     {
/* 397 */       synchronized (this.mHandlerMap) {
/* 398 */         handler = (ChunkHandler)this.mHandlerMap.get(Integer.valueOf(type));
/* 399 */         reply = false;
       }
     }
 
/* 403 */     if (handler == null) {
/* 404 */       Log.w("ddms", new StringBuilder().append("Received unsupported chunk type ").append(ChunkHandler.name(type)).append(" (len=").append(length).append(")").toString());
     }
     else {
/* 407 */       Log.d("ddms", new StringBuilder().append("Calling handler for ").append(ChunkHandler.name(type)).append(" [").append(handler).append("] (len=").append(length).append(")").toString());
 
/* 409 */       ByteBuffer ibuf = buf.slice();
/* 410 */       ByteBuffer roBuf = ibuf.asReadOnlyBuffer();
/* 411 */       roBuf.order(ChunkHandler.CHUNK_ORDER);
 
/* 415 */       synchronized (this.mClientList) {
/* 416 */         handler.handleChunk(client, type, roBuf, reply, packet.getId());
       }
     }
   }
 
   synchronized void dropClient(Client client, boolean notify)
   {
/* 428 */     if (sInstance == null) {
/* 429 */       return;
     }
 
/* 432 */     synchronized (this.mClientList) {
/* 433 */       if (!this.mClientList.remove(client)) {
/* 434 */         return;
       }
     }
/* 437 */     client.close(notify);
/* 438 */     broadcast(3, client);
 
/* 444 */     wakeup();
   }
 
   synchronized void dropClients(Collection<? extends Client> clients, boolean notify)
   {
/* 452 */     for (Client c : clients)
/* 453 */       dropClient(c, notify);
   }
 
   private void processDebuggerActivity(SelectionKey key)
   {
/* 462 */     Debugger dbg = (Debugger)key.attachment();
     try
     {
/* 465 */       if (key.isAcceptable())
         try {
/* 467 */           acceptNewDebugger(dbg, null);
         } catch (IOException ioe) {
/* 469 */           Log.w("ddms", "debugger accept() failed");
/* 470 */           ioe.printStackTrace();
         }
/* 472 */       else if (key.isReadable())
/* 473 */         processDebuggerData(key);
       else
/* 475 */         Log.d("ddm-debugger", "key in unknown state");
     }
     catch (CancelledKeyException cke)
     {
     }
   }
 
   private void acceptNewDebugger(Debugger dbg, ServerSocketChannel acceptChan)
     throws IOException
   {
/* 489 */     synchronized (this.mClientList)
     {

       SocketChannel chan;
/* 492 */       if (acceptChan == null)
/* 493 */         chan = dbg.accept();
       else {
/* 495 */         chan = dbg.accept(acceptChan);
       }
/* 497 */       if (chan != null) {
/* 498 */         chan.socket().setTcpNoDelay(true);
 
/* 500 */         wakeup();
         try
         {
/* 503 */           chan.register(this.mSelector, 1, dbg);
         }
         catch (IOException ioe) {
/* 506 */           dbg.closeData();
/* 507 */           throw ioe;
         }
         catch (RuntimeException re) {
/* 510 */           dbg.closeData();
/* 511 */           throw re;
         }
       } else {
/* 514 */         Log.w("ddms", "ignoring duplicate debugger");
       }
     }
   }
 
   private void processDebuggerData(SelectionKey key)
   {
/* 524 */     Debugger dbg = (Debugger)key.attachment();
     try
     {
/* 530 */       dbg.read();
 
/* 536 */       JdwpPacket packet = dbg.getJdwpPacket();
/* 537 */       while (packet != null) {
/* 538 */         Log.v("ddms", new StringBuilder().append("Forwarding dbg req 0x").append(Integer.toHexString(packet.getId())).append(" to ").append(dbg.getClient()).toString());
 
/* 542 */         dbg.forwardPacketToClient(packet);
 
/* 544 */         packet = dbg.getJdwpPacket();
       }
 
     }
     catch (IOException ioe)
     {
/* 557 */       Log.d("ddms", new StringBuilder().append("Closing connection to debugger ").append(dbg).toString());
/* 558 */       dbg.closeData();
/* 559 */       Client client = dbg.getClient();
/* 560 */       if (client.isDdmAware())
       {
/* 562 */         Log.d("ddms", " (recycling client connection as well)");
 
/* 566 */         client.getDeviceImpl().getMonitor().addClientToDropAndReopen(client, -1);
       }
       else {
/* 569 */         Log.d("ddms", " (recycling client connection as well)");
 
/* 572 */         client.getDeviceImpl().getMonitor().addClientToDropAndReopen(client, -1);
       }
     }
   }
 
   private void wakeup()
   {
/* 582 */     this.mSelector.wakeup();
   }
 
   synchronized void quit()
   {
/* 589 */     this.mQuit = true;
/* 590 */     wakeup();
/* 591 */     Log.d("ddms", "Waiting for Monitor thread");
     try {
/* 593 */       join();
 
/* 596 */       synchronized (this.mClientList) {
/* 597 */         for (Client c : this.mClientList) {
/* 598 */           c.close(false);
/* 599 */           broadcast(3, c);
         }
/* 601 */         this.mClientList.clear();
       }
 
/* 604 */       if (this.mDebugSelectedChan != null) {
/* 605 */         this.mDebugSelectedChan.close();
/* 606 */         this.mDebugSelectedChan.socket().close();
/* 607 */         this.mDebugSelectedChan = null;
       }
/* 609 */       this.mSelector.close();
     } catch (InterruptedException ie) {
/* 611 */       ie.printStackTrace();
     }
     catch (IOException e) {
/* 614 */       e.printStackTrace();
     }
 
/* 617 */     sInstance = null;
   }
 
   synchronized void addClient(Client client)
   {
/* 627 */     if (sInstance == null) {
/* 628 */       return;
     }
 
/* 631 */     Log.d("ddms", new StringBuilder().append("Adding new client ").append(client).toString());
 
/* 633 */     synchronized (this.mClientList) {
/* 634 */       this.mClientList.add(client);
       try
       {
/* 645 */         wakeup();
 
/* 647 */         client.register(this.mSelector);
 
/* 649 */         Debugger dbg = client.getDebugger();
/* 650 */         if (dbg != null)
/* 651 */           dbg.registerListener(this.mSelector);
       }
       catch (IOException ioe)
       {
/* 655 */         ioe.printStackTrace();
       }
     }
   }
 
   private void broadcast(int event, Client client)
   {
/* 664 */     Log.d("ddms", new StringBuilder().append("broadcast ").append(event).append(": ").append(client).toString());
     HashSet set;
/* 672 */     synchronized (this.mHandlerMap) {
/* 673 */       Collection values = this.mHandlerMap.values();
/* 674 */       set = new HashSet(values);
     }
 
/* 677 */     Iterator iter = set.iterator();
/* 678 */     while (iter.hasNext()) {
/* 679 */       ChunkHandler handler = (ChunkHandler)iter.next();
/* 680 */       switch (event) {
       case 2:
         try {
/* 683 */           handler.clientReady(client);
         }
         catch (IOException ioe)
         {
/* 692 */           Log.w("ddms", "Got exception while broadcasting 'ready'");
 
/* 694 */           return;
         }
 
       case 3:
/* 698 */         handler.clientDisconnected(client);
/* 699 */         break;
       default:
/* 701 */         throw new UnsupportedOperationException();
       }
     }
   }
 
   private boolean reopenDebugSelectedPort()
     throws IOException
   {
/* 714 */     Log.d("ddms", new StringBuilder().append("reopen debug-selected port: ").append(this.mNewDebugSelectedPort).toString());
/* 715 */     if (this.mDebugSelectedChan != null) {
/* 716 */       this.mDebugSelectedChan.close();
     }
 
/* 719 */     this.mDebugSelectedChan = ServerSocketChannel.open();
/* 720 */     this.mDebugSelectedChan.configureBlocking(false);
 
/* 722 */     InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), this.mNewDebugSelectedPort);
 
/* 725 */     this.mDebugSelectedChan.socket().setReuseAddress(true);
     try
     {
/* 728 */       this.mDebugSelectedChan.socket().bind(addr);
/* 729 */       if (this.mSelectedClient != null) {
/* 730 */         this.mSelectedClient.update(4);
       }
 
/* 733 */       this.mDebugSelectedChan.register(this.mSelector, 16, this);
 
/* 735 */       return true;
     } catch (BindException e) {
/* 737 */       displayDebugSelectedBindError(this.mNewDebugSelectedPort);
 
/* 740 */       this.mDebugSelectedChan = null;
/* 741 */       this.mNewDebugSelectedPort = -1;
     }
/* 743 */     return false;
   }
 
   private void processDebugSelectedActivity(SelectionKey key)
   {
/* 751 */     assert (key.isAcceptable());
 
/* 753 */     ServerSocketChannel acceptChan = (ServerSocketChannel)key.channel();
 
/* 758 */     if (this.mSelectedClient != null) {
/* 759 */       Debugger dbg = this.mSelectedClient.getDebugger();
 
/* 761 */       if (dbg != null) {
/* 762 */         Log.d("ddms", "Accepting connection on 'debug selected' port");
         try {
/* 764 */           acceptNewDebugger(dbg, acceptChan);
         }
         catch (IOException ioe)
         {
         }
/* 769 */         return;
       }
     }
 
/* 773 */     Log.w("ddms", "Connection on 'debug selected' port, but none selected");
     try
     {
/* 776 */       SocketChannel chan = acceptChan.accept();
/* 777 */       chan.close();
     } catch (IOException ioe) {
     }
     catch (NotYetBoundException e) {
/* 781 */       displayDebugSelectedBindError(this.mDebugSelectedPort);
     }
   }
 
   private void displayDebugSelectedBindError(int port) {
/* 786 */     String message = String.format("Could not open Selected VM debug port (%1$d). Make sure you do not have another instance of DDMS or of the eclipse plugin running. If it's being used by something else, choose a new port number in the preferences.", new Object[] { Integer.valueOf(port) });
 
/* 790 */     Log.logAndDisplay(Log.LogLevel.ERROR, "ddms", message);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.MonitorThread
 * JD-Core Version:    0.6.2
 */