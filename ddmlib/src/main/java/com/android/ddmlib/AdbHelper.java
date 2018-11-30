 package com.android.ddmlib;
 
 import com.android.ddmlib.log.LogReceiver;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.UnsupportedEncodingException;
 import java.net.InetSocketAddress;
 import java.net.Socket;
 import java.nio.ByteBuffer;
 import java.nio.ByteOrder;
 import java.nio.channels.SocketChannel;
 import java.util.concurrent.TimeUnit;
 
 final class AdbHelper
 {
   static final int WAIT_TIME = 5;
   static final String DEFAULT_ENCODING = "ISO-8859-1";
 
   public static SocketChannel open(InetSocketAddress adbSockAddr, Device device, int devicePort)
     throws IOException, TimeoutException, AdbCommandRejectedException
   {
/*  80 */     SocketChannel adbChan = SocketChannel.open(adbSockAddr);
     try {
/*  82 */       adbChan.socket().setTcpNoDelay(true);
/*  83 */       adbChan.configureBlocking(false);
 
/*  87 */       setDevice(adbChan, device);
 
/*  89 */       byte[] req = createAdbForwardRequest(null, devicePort);
 
/*  92 */       write(adbChan, req);
 
/*  94 */       AdbResponse resp = readAdbResponse(adbChan, false);
/*  95 */       if (!resp.okay) {
/*  96 */         throw new AdbCommandRejectedException(resp.message);
       }
 
/*  99 */       adbChan.configureBlocking(true);
     } catch (TimeoutException e) {
/* 101 */       adbChan.close();
/* 102 */       throw e;
     } catch (IOException e) {
/* 104 */       adbChan.close();
/* 105 */       throw e;
     } catch (AdbCommandRejectedException e) {
/* 107 */       adbChan.close();
/* 108 */       throw e;
     }
 
/* 111 */     return adbChan;
   }
 
   public static SocketChannel createPassThroughConnection(InetSocketAddress adbSockAddr, Device device, int pid)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 129 */     SocketChannel adbChan = SocketChannel.open(adbSockAddr);
     try {
/* 131 */       adbChan.socket().setTcpNoDelay(true);
/* 132 */       adbChan.configureBlocking(false);
 
/* 136 */       setDevice(adbChan, device);
 
/* 138 */       byte[] req = createJdwpForwardRequest(pid);
 
/* 141 */       write(adbChan, req);
 
/* 143 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 144 */       if (!resp.okay) {
/* 145 */         throw new AdbCommandRejectedException(resp.message);
       }
 
/* 148 */       adbChan.configureBlocking(true);
     } catch (TimeoutException e) {
/* 150 */       adbChan.close();
/* 151 */       throw e;
     } catch (IOException e) {
/* 153 */       adbChan.close();
/* 154 */       throw e;
     } catch (AdbCommandRejectedException e) {
/* 156 */       adbChan.close();
/* 157 */       throw e;
     }
 
/* 160 */     return adbChan;
   }
 
   private static byte[] createAdbForwardRequest(String addrStr, int port)
   {

     String reqStr;
/* 172 */     if (addrStr == null)
/* 173 */       reqStr = "tcp:" + port;
     else
/* 175 */       reqStr = "tcp:" + port + ":" + addrStr;
/* 176 */     return formAdbRequest(reqStr);
   }
 
   private static byte[] createJdwpForwardRequest(int pid)
   {
/* 185 */     String reqStr = String.format("jdwp:%1$d", new Object[] { Integer.valueOf(pid) });
/* 186 */     return formAdbRequest(reqStr);
   }
 
   public static byte[] formAdbRequest(String req)
   {
/* 195 */     String resultStr = String.format("%04X%s", new Object[] { Integer.valueOf(req.length()), req });
     byte[] result;
     try
     {
/* 198 */       result = resultStr.getBytes("ISO-8859-1");
     } catch (UnsupportedEncodingException uee) {
/* 200 */       uee.printStackTrace();
/* 201 */       return null;
     }
/* 203 */     assert (result.length == req.length() + 4);
/* 204 */     return result;
   }
 
   static AdbResponse readAdbResponse(SocketChannel chan, boolean readDiagString)
     throws TimeoutException, IOException
   {
/* 219 */     AdbResponse resp = new AdbResponse();
 
/* 221 */     byte[] reply = new byte[4];
/* 222 */     read(chan, reply);
 
/* 224 */     if (isOkay(reply)) {
/* 225 */       resp.okay = true;
     } else {
/* 227 */       readDiagString = true;
/* 228 */       resp.okay = false;
     }
 
/* 233 */     label246: 
     try { if (readDiagString) {
/* 235 */         byte[] lenBuf = new byte[4];
/* 236 */         read(chan, lenBuf);
 
/* 238 */         String lenStr = replyToString(lenBuf);
         int len;
         try { len = Integer.parseInt(lenStr, 16);
         } catch (NumberFormatException nfe) {
/* 244 */           Log.w("ddms", "Expected digits, got '" + lenStr + "': " + lenBuf[0] + " " + lenBuf[1] + " " + lenBuf[2] + " " + lenBuf[3]);
 
/* 247 */           Log.w("ddms", "reply was " + replyToString(reply));
/* 248 */           break label246;
         }
 
/* 251 */         byte[] msg = new byte[len];
/* 252 */         read(chan, msg);
 
/* 254 */         resp.message = replyToString(msg);
/* 255 */         Log.v("ddms", "Got reply '" + replyToString(reply) + "', diag='" + resp.message + "'");
       }
 
     }
     catch (Exception e)
     {
     }
 
/* 265 */     return resp;
   }
 
   static RawImage getFrameBuffer(InetSocketAddress adbSockAddr, Device device, long timeout, TimeUnit unit)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 279 */     RawImage imageParams = new RawImage();
/* 280 */     byte[] request = formAdbRequest("framebuffer:");
/* 281 */     byte[] nudge = { 0 };
 
/* 286 */     SocketChannel adbChan = null;
     try {
/* 288 */       adbChan = SocketChannel.open(adbSockAddr);
/* 289 */       adbChan.configureBlocking(false);
 
/* 293 */       setDevice(adbChan, device);
 
/* 295 */       write(adbChan, request);
 
/* 297 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 298 */       if (!resp.okay) {
/* 299 */         throw new AdbCommandRejectedException(resp.message);
       }
 
/* 303 */       byte[] reply = new byte[4];
/* 304 */       read(adbChan, reply);
 
/* 306 */       ByteBuffer buf = ByteBuffer.wrap(reply);
/* 307 */       buf.order(ByteOrder.LITTLE_ENDIAN);
 
/* 309 */       int version = buf.getInt();
 
/* 312 */       int headerSize = RawImage.getHeaderSize(version);
 
/* 315 */       reply = new byte[headerSize * 4];
/* 316 */       read(adbChan, reply);
 
/* 318 */       buf = ByteBuffer.wrap(reply);
/* 319 */       buf.order(ByteOrder.LITTLE_ENDIAN);
 
/* 322 */       if (!imageParams.readHeader(version, buf)) {
/* 323 */         Log.e("Screenshot", "Unsupported protocol: " + version);
/* 324 */         return null;
       }
 
/* 327 */       Log.d("ddms", "image params: bpp=" + imageParams.bpp + ", size=" + imageParams.size + ", width=" + imageParams.width + ", height=" + imageParams.height);
 
/* 331 */       write(adbChan, nudge);
 
/* 333 */       reply = new byte[imageParams.size];
/* 334 */       read(adbChan, reply, imageParams.size, unit.toMillis(timeout));
 
/* 336 */       imageParams.data = reply;
     } finally {
/* 338 */       if (adbChan != null) {
/* 339 */         adbChan.close();
       }
     }
 
/* 343 */     return imageParams;
   }
   @Deprecated
   static void executeConnectCommand(InetSocketAddress adbSockAddr, String port, IDevice device, IShellOutputReceiver rcvr, int maxTimeToOutputResponse)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     /* 353 */     executeConnectCommand(adbSockAddr, port, device, rcvr, maxTimeToOutputResponse, TimeUnit.MILLISECONDS);
   }
   static void executeConnectCommand(InetSocketAddress adbSockAddr, String port, IDevice device, IShellOutputReceiver rcvr, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     /* 382 */     executeRemoteCommand(adbSockAddr, AdbService.CONNECT, port, device, rcvr, maxTimeToOutputResponse, maxTimeUnits, null);
   }
   @Deprecated
   static void executeTCPIPCommand(InetSocketAddress adbSockAddr, String port, IDevice device, IShellOutputReceiver rcvr, int maxTimeToOutputResponse)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     /* 353 */     executeTCPIPCommand(adbSockAddr, port, device, rcvr, maxTimeToOutputResponse, TimeUnit.MILLISECONDS);
   }
   static void executeTCPIPCommand(InetSocketAddress adbSockAddr, String port, IDevice device, IShellOutputReceiver rcvr, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
           throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
     /* 382 */     executeRemoteCommand(adbSockAddr, AdbService.TCPIP, port, device, rcvr, maxTimeToOutputResponse, maxTimeUnits, null);
   }
   @Deprecated
   static void executeRemoteCommand(InetSocketAddress adbSockAddr, String command, IDevice device, IShellOutputReceiver rcvr, int maxTimeToOutputResponse)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 353 */     executeRemoteCommand(adbSockAddr, command, device, rcvr, maxTimeToOutputResponse, TimeUnit.MILLISECONDS);
   }

   static void executeRemoteCommand(InetSocketAddress adbSockAddr, String command, IDevice device, IShellOutputReceiver rcvr, long maxTimeToOutputResponse, TimeUnit maxTimeUnits)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 382 */     executeRemoteCommand(adbSockAddr, AdbService.SHELL, command, device, rcvr, maxTimeToOutputResponse, maxTimeUnits, null);
   }
 
   static void executeRemoteCommand(InetSocketAddress adbSockAddr, AdbService adbService, String command, IDevice device, IShellOutputReceiver rcvr, long maxTimeToOutputResponse, TimeUnit maxTimeUnits, InputStream is)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 434 */     long maxTimeToOutputMs = 0L;
/* 435 */     if (maxTimeToOutputResponse > 0L) {
/* 436 */       if (maxTimeUnits == null) {
/* 437 */         throw new NullPointerException("Time unit must not be null for non-zero max.");
       }
/* 439 */       maxTimeToOutputMs = maxTimeUnits.toMillis(maxTimeToOutputResponse);
     }
 
/* 442 */     Log.e("ddms", "execute: running " + command);
 
/* 444 */     SocketChannel adbChan = null;
     try {
/* 446 */       adbChan = SocketChannel.open(adbSockAddr);
/* 447 */       adbChan.configureBlocking(false);
 
/* 451 */       setDevice(adbChan, device);
 
/* 453 */       byte[] request = formAdbRequest(adbService.name().toLowerCase() + ":" + command);
/* 454 */       write(adbChan, request);
 
/* 456 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 457 */       if (!resp.okay) {
/* 458 */         Log.e("ddms", "ADB rejected shell command (" + command + "): " + resp.message);
/* 459 */         throw new AdbCommandRejectedException(resp.message);
       }
 
/* 462 */       byte[] data = new byte[16384];
 
/* 465 */       if (is != null)
       {
         int read;
/* 467 */         while ((read = is.read(data)) != -1) {
/* 468 */           ByteBuffer buf = ByteBuffer.wrap(data, 0, read);
/* 469 */           int written = 0;
/* 470 */           while (buf.hasRemaining()) {
/* 471 */             written += adbChan.write(buf);
           }
/* 473 */           if (written != read) {
/* 474 */             Log.e("ddms", "ADB write inconsistency, wrote " + written + "expected " + read);
 
/* 476 */             throw new AdbCommandRejectedException("write failed");
           }
         }
       }
 
/* 481 */       ByteBuffer buf = ByteBuffer.wrap(data);
/* 482 */       buf.clear();
/* 483 */       long timeToResponseCount = 0L;
       while (true)
       {
/* 487 */         if ((rcvr != null) && (rcvr.isCancelled())) {
/* 488 */           Log.v("ddms", "execute: cancelled");
/* 489 */           break;
         }
 
/* 492 */         int count = adbChan.read(buf);
/* 493 */         if (count < 0)
         {
/* 495 */           rcvr.flush();
/* 496 */           Log.v("ddms", "execute '" + command + "' on '" + device + "' : EOF hit. Read: " + count);
 
/* 498 */           break;
/* 499 */         }if (count == 0) {
           try {
/* 501 */             int wait = 25;
/* 502 */             timeToResponseCount += wait;
/* 503 */             if ((maxTimeToOutputMs > 0L) && (timeToResponseCount > maxTimeToOutputMs)) {
/* 504 */               throw new ShellCommandUnresponsiveException();
             }
/* 506 */             Thread.sleep(wait);
           }
           catch (InterruptedException e) {
/* 509 */             Thread.currentThread().interrupt();
 
/* 511 */             throw new TimeoutException("executeRemoteCommand interrupted with immediate timeout via interruption.");
           }
         }
         else {
/* 515 */           timeToResponseCount = 0L;
 
/* 518 */           if (rcvr != null) {
/* 519 */             rcvr.addOutput(buf.array(), buf.arrayOffset(), buf.position());
           }
/* 521 */           buf.rewind();
         }
       }
     } finally {
/* 525 */       if (adbChan != null) {
/* 526 */         adbChan.close();
       }
/* 528 */       Log.v("ddms", "execute: returning");
     }
   }
 
   public static void runEventLogService(InetSocketAddress adbSockAddr, Device device, LogReceiver rcvr)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 545 */     runLogService(adbSockAddr, device, "events", rcvr);
   }
 
   public static void runLogService(InetSocketAddress adbSockAddr, Device device, String logName, LogReceiver rcvr)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 561 */     SocketChannel adbChan = null;
     try
     {
/* 564 */       adbChan = SocketChannel.open(adbSockAddr);
/* 565 */       adbChan.configureBlocking(false);
 
/* 569 */       setDevice(adbChan, device);
 
/* 571 */       byte[] request = formAdbRequest("log:" + logName);
/* 572 */       write(adbChan, request);
 
/* 574 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 575 */       if (!resp.okay) {
/* 576 */         throw new AdbCommandRejectedException(resp.message);
       }
 
/* 579 */       byte[] data = new byte[16384];
/* 580 */       ByteBuffer buf = ByteBuffer.wrap(data);
 
/* 584 */       while ((rcvr == null) || (!rcvr.isCancelled()))
       {
/* 588 */         int count = adbChan.read(buf);
/* 589 */         if (count < 0)
           break;
/* 591 */         if (count == 0) {
           try {
/* 593 */             Thread.sleep(25L);
           }
           catch (InterruptedException e) {
/* 596 */             Thread.currentThread().interrupt();
 
/* 598 */             throw new TimeoutException("runLogService interrupted with immediate timeout via interruption.");
           }
         } else {
/* 601 */           if (rcvr != null) {
/* 602 */             rcvr.parseNewData(buf.array(), buf.arrayOffset(), buf.position());
           }
/* 604 */           buf.rewind();
         }
       }
     } finally {
/* 608 */       if (adbChan != null)
/* 609 */         adbChan.close();
     }
   }
   //host is  '192.168.1.101:5555'
   public static void createConnection(InetSocketAddress adbSockAddr, Device device, String host)
           throws TimeoutException, AdbCommandRejectedException, IOException
   {
     /* 635 */     SocketChannel adbChan = null;
     try {
       /* 637 */       adbChan = SocketChannel.open(adbSockAddr);
       /* 638 */       adbChan.configureBlocking(false);

       /* 640 */       byte[] request = formAdbRequest(String.format("host-serial:%1$s:connect:%2$s", new Object[] { device.getSerialNumber(), host}));

       /* 644 */       write(adbChan, request);

       /* 646 */       AdbResponse resp = readAdbResponse(adbChan, false);
       /* 647 */       if (!resp.okay) {
         /* 648 */         Log.w("adb  connect", "Error build connection: " + resp.message);
         /* 649 */         throw new AdbCommandRejectedException(resp.message);
       }
     } finally {
       /* 652 */       if (adbChan != null)
         /* 653 */         adbChan.close();
     }
   }
   public static void createForward(InetSocketAddress adbSockAddr, Device device, String localPortSpec, String remotePortSpec)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 635 */     SocketChannel adbChan = null;
     try {
/* 637 */       adbChan = SocketChannel.open(adbSockAddr);
/* 638 */       adbChan.configureBlocking(false);
 
/* 640 */       byte[] request = formAdbRequest(String.format("host-serial:%1$s:forward:%2$s;%3$s", new Object[] { device.getSerialNumber(), localPortSpec, remotePortSpec }));
 
/* 644 */       write(adbChan, request);
 
/* 646 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 647 */       if (!resp.okay) {
/* 648 */         Log.w("create-forward", "Error creating forward: " + resp.message);
/* 649 */         throw new AdbCommandRejectedException(resp.message);
       }
     } finally {
/* 652 */       if (adbChan != null)
/* 653 */         adbChan.close();
     }
   }
 
   public static void removeForward(InetSocketAddress adbSockAddr, Device device, String localPortSpec, String remotePortSpec)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 679 */     SocketChannel adbChan = null;
     try {
/* 681 */       adbChan = SocketChannel.open(adbSockAddr);
/* 682 */       adbChan.configureBlocking(false);
 
/* 684 */       byte[] request = formAdbRequest(String.format("host-serial:%1$s:killforward:%2$s", new Object[] { device.getSerialNumber(), localPortSpec }));
 
/* 688 */       write(adbChan, request);
 
/* 690 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 691 */       if (!resp.okay) {
/* 692 */         Log.w("remove-forward", "Error creating forward: " + resp.message);
/* 693 */         throw new AdbCommandRejectedException(resp.message);
       }
     } finally {
/* 696 */       if (adbChan != null)
/* 697 */         adbChan.close();
     }
   }
 
   static boolean isOkay(byte[] reply)
   {
/* 706 */     return (reply[0] == 79) && (reply[1] == 75) && (reply[2] == 65) && (reply[3] == 89);
   }
 
   static String replyToString(byte[] reply)
   {
     String result;
     try
     {
/* 716 */       result = new String(reply, "ISO-8859-1");
     } catch (UnsupportedEncodingException uee) {
/* 718 */       uee.printStackTrace();
/* 719 */       result = "";
     }
/* 721 */     return result;
   }
 
   static void read(SocketChannel chan, byte[] data)
     throws TimeoutException, IOException
   {
/* 736 */     read(chan, data, -1, DdmPreferences.getTimeOut());
   }
 
   static void read(SocketChannel chan, byte[] data, int length, long timeout)
     throws TimeoutException, IOException
   {
/* 753 */     ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
/* 754 */     int numWaits = 0;
 
/* 756 */     while (buf.position() != buf.limit())
     {
/* 759 */       int count = chan.read(buf);
/* 760 */       if (count < 0) {
/* 761 */         Log.d("ddms", "read: channel EOF");
/* 762 */         throw new IOException("EOF");
/* 763 */       }if (count == 0)
       {
/* 765 */         if ((timeout != 0L) && (numWaits * 5 > timeout)) {
/* 766 */           Log.d("ddms", "read: timeout");
/* 767 */           throw new TimeoutException();
         }
         try
         {
/* 771 */           Thread.sleep(5L);
         }
         catch (InterruptedException e) {
/* 774 */           Thread.currentThread().interrupt();
 
/* 776 */           throw new TimeoutException("Read interrupted with immediate timeout via interruption.");
         }
/* 778 */         numWaits++;
       } else {
/* 780 */         numWaits = 0;
       }
     }
   }
 
   static void write(SocketChannel chan, byte[] data)
     throws TimeoutException, IOException
   {
/* 794 */     write(chan, data, -1, DdmPreferences.getTimeOut());
   }
 
   static void write(SocketChannel chan, byte[] data, int length, int timeout)
     throws TimeoutException, IOException
   {
/* 809 */     ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
/* 810 */     int numWaits = 0;
 
/* 812 */     while (buf.position() != buf.limit())
     {
/* 815 */       int count = chan.write(buf);
/* 816 */       if (count < 0) {
/* 817 */         Log.d("ddms", "write: channel EOF");
/* 818 */         throw new IOException("channel EOF");
/* 819 */       }if (count == 0)
       {
/* 821 */         if ((timeout != 0) && (numWaits * 5 > timeout)) {
/* 822 */           Log.d("ddms", "write: timeout");
/* 823 */           throw new TimeoutException();
         }
         try
         {
/* 827 */           Thread.sleep(5L);
         }
         catch (InterruptedException e) {
/* 830 */           Thread.currentThread().interrupt();
 
/* 832 */           throw new TimeoutException("Write interrupted with immediate timeout via interruption.");
         }
/* 834 */         numWaits++;
       } else {
/* 836 */         numWaits = 0;
       }
     }
   }
 
   static void setDevice(SocketChannel adbChan, IDevice device)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 854 */     if (device != null) {
/* 855 */       String msg = "host:transport:" + device.getSerialNumber();
/* 856 */       byte[] device_query = formAdbRequest(msg);
 
/* 858 */       write(adbChan, device_query);
 
/* 860 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 861 */       if (!resp.okay)
/* 862 */         throw new AdbCommandRejectedException(resp.message, true);
     }
   }
 
   public static void reboot(String into, InetSocketAddress adbSockAddr, Device device)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {

     byte[] request;
/* 879 */     if (into == null)
/* 880 */       request = formAdbRequest("reboot:");
     else {
/* 882 */       request = formAdbRequest("reboot:" + into);
     }
 
/* 885 */     SocketChannel adbChan = null;
     try {
/* 887 */       adbChan = SocketChannel.open(adbSockAddr);
/* 888 */       adbChan.configureBlocking(false);
 
/* 892 */       setDevice(adbChan, device);
 
/* 894 */       write(adbChan, request);
     } finally {
/* 896 */       if (adbChan != null)
/* 897 */         adbChan.close();
     }
   }
 
   public static void root(InetSocketAddress adbSockAddr, Device device)
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
/* 916 */     byte[] request = formAdbRequest("root:");
/* 917 */     SocketChannel adbChan = null;
     try {
/* 919 */       adbChan = SocketChannel.open(adbSockAddr);
/* 920 */       adbChan.configureBlocking(false);
 
/* 924 */       setDevice(adbChan, device);
 
/* 926 */       write(adbChan, request);
 
/* 928 */       AdbResponse resp = readAdbResponse(adbChan, false);
/* 929 */       if (!resp.okay) {
/* 930 */         Log.w("root", "Error setting root: " + resp.message);
/* 931 */         throw new AdbCommandRejectedException(resp.message);
       }
     }
     finally {
/* 935 */       if (adbChan != null)
/* 936 */         adbChan.close();
     }
   }
 
   public static enum AdbService
   {
/* 393 */     SHELL, 
 
/* 398 */     EXEC,
              TCPIP,
              FORWARD,
              CONNECT
   }
 
   static class AdbResponse
   {
     public boolean okay;
     public String message;
 
     public AdbResponse()
     {
/*  57 */       this.message = "";
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.AdbHelper
 * JD-Core Version:    0.6.2
 */