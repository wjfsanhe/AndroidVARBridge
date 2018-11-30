 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.InetAddress;
 import java.net.InetSocketAddress;
 import java.nio.ByteBuffer;
 import java.nio.channels.SocketChannel;
 import java.security.InvalidParameterException;
 import java.util.Formatter;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public final class EmulatorConsole
 {
   private static final String DEFAULT_ENCODING = "ISO-8859-1";
   private static final int WAIT_TIME = 5;
   private static final int STD_TIMEOUT = 5000;
   private static final String HOST = "127.0.0.1";
   private static final String COMMAND_PING = "help\r\n";
   private static final String COMMAND_AVD_NAME = "avd name\r\n";
   private static final String COMMAND_KILL = "kill\r\n";
   private static final String COMMAND_GSM_STATUS = "gsm status\r\n";
   private static final String COMMAND_GSM_CALL = "gsm call %1$s\r\n";
   private static final String COMMAND_GSM_CANCEL_CALL = "gsm cancel %1$s\r\n";
   private static final String COMMAND_GSM_DATA = "gsm data %1$s\r\n";
   private static final String COMMAND_GSM_VOICE = "gsm voice %1$s\r\n";
   private static final String COMMAND_SMS_SEND = "sms send %1$s %2$s\r\n";
   private static final String COMMAND_NETWORK_STATUS = "network status\r\n";
   private static final String COMMAND_NETWORK_SPEED = "network speed %1$s\r\n";
   private static final String COMMAND_NETWORK_LATENCY = "network delay %1$s\r\n";
   private static final String COMMAND_GPS = "geo fix %1$f %2$f %3$f\r\n";
/*  74 */   private static final Pattern RE_KO = Pattern.compile("KO:\\s+(.*)");
 
/*  79 */   public static final int[] MIN_LATENCIES = { 0, 150, 80, 35 };
 
/*  89 */   public static final int[] DOWNLOAD_SPEEDS = { 0, 14400, 43200, 80000, 236800, 1920000, 14400000 };
 
/* 100 */   public static final String[] NETWORK_SPEEDS = { "full", "gsm", "hscsd", "gprs", "edge", "umts", "hsdpa" };
 
/* 111 */   public static final String[] NETWORK_LATENCIES = { "none", "gprs", "edge", "umts" };
 
/* 163 */   public static final String RESULT_OK = null;
 
/* 165 */   private static final Pattern sEmulatorRegexp = Pattern.compile("emulator-(\\d+)");
/* 166 */   private static final Pattern sVoiceStatusRegexp = Pattern.compile("gsm\\s+voice\\s+state:\\s*([a-z]+)", 2);
 
/* 168 */   private static final Pattern sDataStatusRegexp = Pattern.compile("gsm\\s+data\\s+state:\\s*([a-z]+)", 2);
 
/* 170 */   private static final Pattern sDownloadSpeedRegexp = Pattern.compile("\\s+download\\s+speed:\\s+(\\d+)\\s+bits.*", 2);
 
/* 172 */   private static final Pattern sMinLatencyRegexp = Pattern.compile("\\s+minimum\\s+latency:\\s+(\\d+)\\s+ms", 2);
 
/* 176 */   private static final HashMap<Integer, EmulatorConsole> sEmulators = new HashMap();
   private static final String LOG_TAG = "EmulatorConsole";
/* 197 */   private int mPort = -1;
   private SocketChannel mSocketChannel;
/* 201 */   private byte[] mBuffer = new byte[1024];
 
   public static EmulatorConsole getConsole(IDevice d)
   {
/* 216 */     Integer port = getEmulatorPort(d.getSerialNumber());
/* 217 */     if (port == null) {
/* 218 */       Log.w("EmulatorConsole", "Failed to find emulator port from serial: " + d.getSerialNumber());
/* 219 */       return null;
     }
 
/* 222 */     EmulatorConsole console = retrieveConsole(port.intValue());
 
/* 224 */     if (!console.checkConnection()) {
/* 225 */       removeConsole(console.mPort);
/* 226 */       console = null;
     }
 
/* 229 */     return console;
   }
 
   public static Integer getEmulatorPort(String serialNumber)
   {
/* 239 */     Matcher m = sEmulatorRegexp.matcher(serialNumber);
/* 240 */     if (m.matches())
     {
       try
       {
/* 244 */         int port = Integer.parseInt(m.group(1));
/* 245 */         if (port > 0) {
/* 246 */           return Integer.valueOf(port);
         }
       }
       catch (NumberFormatException e)
       {
       }
     }
 
/* 254 */     return null;
   }
 
   private static EmulatorConsole retrieveConsole(int port)
   {
/* 262 */     synchronized (sEmulators) {
/* 263 */       EmulatorConsole console = (EmulatorConsole)sEmulators.get(Integer.valueOf(port));
/* 264 */       if (console == null) {
/* 265 */         Log.v("EmulatorConsole", "Creating emulator console for " + Integer.toString(port));
/* 266 */         console = new EmulatorConsole(port);
/* 267 */         sEmulators.put(Integer.valueOf(port), console);
       }
/* 269 */       return console;
     }
   }
 
   private static void removeConsole(int port)
   {
/* 278 */     synchronized (sEmulators) {
/* 279 */       Log.v("EmulatorConsole", "Removing emulator console for " + Integer.toString(port));
/* 280 */       sEmulators.remove(Integer.valueOf(port));
     }
   }
 
   private EmulatorConsole(int port) {
/* 285 */     this.mPort = port;
   }
 
   private synchronized boolean checkConnection()
   {
/* 294 */     if (this.mSocketChannel == null)
     {
       try
       {
/* 298 */         InetAddress hostAddr = InetAddress.getByName("127.0.0.1");
/* 299 */         InetSocketAddress socketAddr = new InetSocketAddress(hostAddr, this.mPort);
/* 300 */         this.mSocketChannel = SocketChannel.open(socketAddr);
/* 301 */         this.mSocketChannel.configureBlocking(false);
 
/* 303 */         readLines();
       } catch (IOException e) {
/* 305 */         Log.w("EmulatorConsole", "Failed to start Emulator console for " + Integer.toString(this.mPort));
/* 306 */         return false;
       }
     }
 
/* 310 */     return ping();
   }
 
   private synchronized boolean ping()
   {
/* 320 */     if (sendCommand("help\r\n")) {
/* 321 */       return readLines() != null;
     }
 
/* 324 */     return false;
   }
 
   public synchronized void kill()
   {
/* 331 */     if (sendCommand("kill\r\n"))
/* 332 */       close();
   }
 
   public synchronized void close()
   {
/* 340 */     if (this.mPort == -1) {
/* 341 */       return;
     }
 
/* 344 */     removeConsole(this.mPort);
     try {
/* 346 */       if (this.mSocketChannel != null) {
/* 347 */         this.mSocketChannel.close();
       }
/* 349 */       this.mSocketChannel = null;
/* 350 */       this.mPort = -1;
     } catch (IOException e) {
/* 352 */       Log.w("EmulatorConsole", "Failed to close EmulatorConsole channel");
     }
   }
 
   public synchronized String getAvdName() {
/* 357 */     if (sendCommand("avd name\r\n")) {
/* 358 */       String[] result = readLines();
/* 359 */       if ((result != null) && (result.length == 2))
       {
/* 361 */         return result[0];
       }
 
/* 364 */       Matcher m = RE_KO.matcher(result[(result.length - 1)]);
/* 365 */       if (m.matches()) {
/* 366 */         return m.group(1);
       }
/* 368 */       Log.w("EmulatorConsole", "avd name result did not match expected");
/* 369 */       for (int i = 0; i < result.length; i++) {
/* 370 */         Log.d("EmulatorConsole", result[i]);
       }
 
     }
 
/* 375 */     return null;
   }
 
   public synchronized NetworkStatus getNetworkStatus()
   {
/* 384 */     if (sendCommand("network status\r\n"))
     {
/* 392 */       String[] result = readLines();
 
/* 394 */       if (isValid(result))
       {
/* 398 */         NetworkStatus status = new NetworkStatus();
/* 399 */         for (String line : result) {
/* 400 */           Matcher m = sDownloadSpeedRegexp.matcher(line);
/* 401 */           if (m.matches())
           {
/* 403 */             String value = m.group(1);
 
/* 406 */             status.speed = getSpeedIndex(value);
           }
           else
           {
/* 412 */             m = sMinLatencyRegexp.matcher(line);
/* 413 */             if (m.matches())
             {
/* 415 */               String value = m.group(1);
 
/* 418 */               status.latency = getLatencyIndex(value);
             }
 
           }
 
         }
 
/* 425 */         return status;
       }
     }
 
/* 429 */     return null;
   }
 
   public synchronized GsmStatus getGsmStatus()
   {
/* 438 */     if (sendCommand("gsm status\r\n"))
     {
/* 446 */       String[] result = readLines();
/* 447 */       if (isValid(result))
       {
/* 449 */         GsmStatus status = new GsmStatus();
 
/* 453 */         for (String line : result) {
/* 454 */           Matcher m = sVoiceStatusRegexp.matcher(line);
/* 455 */           if (m.matches())
           {
/* 457 */             String value = m.group(1);
 
/* 460 */             status.voice = GsmMode.getEnum(value.toLowerCase(Locale.US));
           }
           else
           {
/* 466 */             m = sDataStatusRegexp.matcher(line);
/* 467 */             if (m.matches())
             {
/* 469 */               String value = m.group(1);
 
/* 472 */               status.data = GsmMode.getEnum(value.toLowerCase(Locale.US));
             }
 
           }
 
         }
 
/* 479 */         return status;
       }
     }
 
/* 483 */     return null;
   }
 
   public synchronized String setGsmVoiceMode(GsmMode mode)
     throws InvalidParameterException
   {
/* 493 */     if (mode == GsmMode.UNKNOWN) {
/* 494 */       throw new InvalidParameterException();
     }
 
/* 497 */     String command = String.format("gsm voice %1$s\r\n", new Object[] { mode.getTag() });
/* 498 */     return processCommand(command);
   }
 
   public synchronized String setGsmDataMode(GsmMode mode)
     throws InvalidParameterException
   {
/* 508 */     if (mode == GsmMode.UNKNOWN) {
/* 509 */       throw new InvalidParameterException();
     }
 
/* 512 */     String command = String.format("gsm data %1$s\r\n", new Object[] { mode.getTag() });
/* 513 */     return processCommand(command);
   }
 
   public synchronized String call(String number)
   {
/* 522 */     String command = String.format("gsm call %1$s\r\n", new Object[] { number });
/* 523 */     return processCommand(command);
   }
 
   public synchronized String cancelCall(String number)
   {
/* 532 */     String command = String.format("gsm cancel %1$s\r\n", new Object[] { number });
/* 533 */     return processCommand(command);
   }
 
   public synchronized String sendSms(String number, String message)
   {
/* 545 */     String command = String.format("sms send %1$s %2$s\r\n", new Object[] { number, message });
/* 546 */     return processCommand(command);
   }
 
   public synchronized String setNetworkSpeed(int selectionIndex)
   {
/* 555 */     String command = String.format("network speed %1$s\r\n", new Object[] { NETWORK_SPEEDS[selectionIndex] });
/* 556 */     return processCommand(command);
   }
 
   public synchronized String setNetworkLatency(int selectionIndex)
   {
/* 565 */     String command = String.format("network delay %1$s\r\n", new Object[] { NETWORK_LATENCIES[selectionIndex] });
/* 566 */     return processCommand(command);
   }
 
   public synchronized String sendLocation(double longitude, double latitude, double elevation)
   {
/* 572 */     Formatter formatter = new Formatter(Locale.US);
     try {
/* 574 */       formatter.format("geo fix %1$f %2$f %3$f\r\n", new Object[] { Double.valueOf(longitude), Double.valueOf(latitude), Double.valueOf(elevation) });
 
/* 576 */       return processCommand(formatter.toString());
     } finally {
/* 578 */       formatter.close();
     }
   }
 
   private boolean sendCommand(String command)
   {
/* 588 */     boolean result = false;
     try {
       byte[] bCommand;
       try {
/* 592 */         bCommand = command.getBytes("ISO-8859-1");
       } catch (UnsupportedEncodingException e) {
/* 594 */         Log.w("EmulatorConsole", "wrong encoding when sending " + command + " to " + Integer.toString(this.mPort));
 
/* 597 */         return result;
       }
 
/* 601 */       AdbHelper.write(this.mSocketChannel, bCommand, bCommand.length, DdmPreferences.getTimeOut());
 
/* 603 */       result = true;
     } catch (Exception e) {
/* 605 */       Log.d("EmulatorConsole", "Exception sending command " + command + " to " + Integer.toString(this.mPort));
 
/* 607 */       return false;
     } finally {
/* 609 */       if (!result)
       {
/* 611 */         removeConsole(this.mPort);
       }
     }
 
/* 615 */     return result;
   }
 
   private String processCommand(String command)
   {
/* 624 */     if (sendCommand(command)) {
/* 625 */       String[] result = readLines();
 
/* 627 */       if ((result != null) && (result.length > 0)) {
/* 628 */         Matcher m = RE_KO.matcher(result[(result.length - 1)]);
/* 629 */         if (m.matches()) {
/* 630 */           return m.group(1);
         }
/* 632 */         return RESULT_OK;
       }
 
/* 635 */       return "Unable to communicate with the emulator";
     }
 
/* 638 */     return "Unable to send command to the emulator";
   }
 
   private String[] readLines()
   {
     try
     {
/* 651 */       ByteBuffer buf = ByteBuffer.wrap(this.mBuffer, 0, this.mBuffer.length);
/* 652 */       int numWaits = 0;
/* 653 */       boolean stop = false;
 
/* 655 */       while ((buf.position() != buf.limit()) && (!stop))
       {
/* 658 */         int count = this.mSocketChannel.read(buf);
/* 659 */         if (count < 0)
/* 660 */           return null;
/* 661 */         if (count == 0) {
/* 662 */           if (numWaits * 5 > 5000) {
/* 663 */             return null;
           }
           try
           {
/* 667 */             Thread.sleep(5L);
           } catch (InterruptedException ie) {
           }
/* 670 */           numWaits++;
         } else {
/* 672 */           numWaits = 0;
         }
 
/* 677 */         if (buf.position() >= 4) {
/* 678 */           int pos = buf.position();
/* 679 */           if ((endsWithOK(pos)) || (lastLineIsKO(pos))) {
/* 680 */             stop = true;
           }
         }
       }
 
/* 685 */       String msg = new String(this.mBuffer, 0, buf.position(), "ISO-8859-1");
/* 686 */       return msg.split("\r\n");
     } catch (IOException e) {
/* 688 */       Log.d("EmulatorConsole", "Exception reading lines for " + Integer.toString(this.mPort));
/* 689 */     }return null;
   }
 
   private boolean endsWithOK(int currentPosition)
   {
/* 698 */     return (this.mBuffer[(currentPosition - 1)] == 10) && (this.mBuffer[(currentPosition - 2)] == 13) && (this.mBuffer[(currentPosition - 3)] == 75) && (this.mBuffer[(currentPosition - 4)] == 79);
   }
 
   private boolean lastLineIsKO(int currentPosition)
   {
/* 711 */     if ((this.mBuffer[(currentPosition - 1)] != 10) || (this.mBuffer[(currentPosition - 2)] != 13))
     {
/* 713 */       return false;
     }
 
/* 717 */     int i = 0;
/* 718 */     for (i = currentPosition - 3; (i >= 0) && (
/* 719 */       (this.mBuffer[i] != 10) || 
/* 721 */       (i <= 0) || (this.mBuffer[(i - 1)] != 13)); i--);
/* 730 */     if ((this.mBuffer[(i + 1)] == 75) && (this.mBuffer[(i + 2)] == 79))
     {
/* 732 */       return true;
     }
 
/* 735 */     return false;
   }
 
   private boolean isValid(String[] result)
   {
/* 742 */     if ((result != null) && (result.length > 0)) {
/* 743 */       return !RE_KO.matcher(result[(result.length - 1)]).matches();
     }
/* 745 */     return false;
   }
 
   private int getLatencyIndex(String value)
   {
     try {
/* 751 */       int latency = Integer.parseInt(value);
 
/* 754 */       for (int i = 0; i < MIN_LATENCIES.length; i++) {
/* 755 */         if (MIN_LATENCIES[i] == latency) {
/* 756 */           return i;
         }
       }
     }
     catch (NumberFormatException e)
     {
     }
/* 763 */     return -1;
   }
 
   private int getSpeedIndex(String value)
   {
     try {
/* 769 */       int speed = Integer.parseInt(value);
 
/* 772 */       for (int i = 0; i < DOWNLOAD_SPEEDS.length; i++) {
/* 773 */         if (DOWNLOAD_SPEEDS[i] == speed) {
/* 774 */           return i;
         }
       }
     }
     catch (NumberFormatException e)
     {
     }
/* 781 */     return -1;
   }
 
   public static class NetworkStatus
   {
/* 192 */     public int speed = -1;
 
/* 194 */     public int latency = -1;
   }
 
   public static class GsmStatus
   {
/* 184 */     public EmulatorConsole.GsmMode voice = EmulatorConsole.GsmMode.UNKNOWN;
 
/* 186 */     public EmulatorConsole.GsmMode data = EmulatorConsole.GsmMode.UNKNOWN;
   }
 
   public static enum GsmMode
   {
/* 120 */     UNKNOWN((String)null), 
/* 121 */     UNREGISTERED(new String[] { "unregistered", "off" }), 
/* 122 */     HOME(new String[] { "home", "on" }), 
/* 123 */     ROAMING("roaming"), 
/* 124 */     SEARCHING("searching"), 
/* 125 */     DENIED("denied");
 
     private final String[] tags;
 
     private GsmMode(String tag) {
/* 130 */       if (tag != null)
/* 131 */         this.tags = new String[] { tag };
       else
/* 133 */         this.tags = new String[0];
     }
 
     private GsmMode(String[] tags)
     {
/* 138 */       this.tags = tags;
     }
 
     public static GsmMode getEnum(String tag) {
/* 142 */       for (GsmMode mode : values()) {
/* 143 */         for (String t : mode.tags) {
/* 144 */           if (t.equals(tag)) {
/* 145 */             return mode;
           }
         }
       }
/* 149 */       return UNKNOWN;
     }
 
     public String getTag()
     {
/* 156 */       if (this.tags.length > 0) {
/* 157 */         return this.tags[0];
       }
/* 159 */       return null;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.EmulatorConsole
 * JD-Core Version:    0.6.2
 */