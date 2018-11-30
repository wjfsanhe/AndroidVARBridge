 package com.android.ddmlib;
 
 import com.android.ddmlib.utils.ArrayHelper;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.net.InetSocketAddress;
 import java.nio.channels.SocketChannel;
 import java.util.ArrayList;
 import java.util.Date;
 
 public class SyncService
 {
/*  43 */   private static final byte[] ID_OKAY = { 79, 75, 65, 89 };
/*  44 */   private static final byte[] ID_FAIL = { 70, 65, 73, 76 };
/*  45 */   private static final byte[] ID_STAT = { 83, 84, 65, 84 };
/*  46 */   private static final byte[] ID_RECV = { 82, 69, 67, 86 };
/*  47 */   private static final byte[] ID_DATA = { 68, 65, 84, 65 };
/*  48 */   private static final byte[] ID_DONE = { 68, 79, 78, 69 };
/*  49 */   private static final byte[] ID_SEND = { 83, 69, 78, 68 };
 
/*  53 */   private static final NullSyncProgressMonitor sNullSyncProgressMonitor = new NullSyncProgressMonitor();
   private static final int S_ISOCK = 49152;
   private static final int S_IFLNK = 40960;
   private static final int S_IFREG = 32768;
   private static final int S_IFBLK = 24576;
   private static final int S_IFDIR = 16384;
   private static final int S_IFCHR = 8192;
   private static final int S_IFIFO = 4096;
   private static final int SYNC_DATA_MAX = 65536;
   private static final int REMOTE_PATH_MAX_LENGTH = 1024;
   private InetSocketAddress mAddress;
   private Device mDevice;
   private SocketChannel mChannel;
   private byte[] mBuffer;
 
   SyncService(InetSocketAddress address, Device device)
   {
/* 177 */     this.mAddress = address;
/* 178 */     this.mDevice = device;
   }
 
   boolean openSync()
     throws TimeoutException, AdbCommandRejectedException, IOException
   {
     try
     {
/* 191 */       this.mChannel = SocketChannel.open(this.mAddress);
/* 192 */       this.mChannel.configureBlocking(false);
 
/* 195 */       AdbHelper.setDevice(this.mChannel, this.mDevice);
 
/* 197 */       byte[] request = AdbHelper.formAdbRequest("sync:");
/* 198 */       AdbHelper.write(this.mChannel, request, -1, DdmPreferences.getTimeOut());
 
/* 200 */       AdbHelper.AdbResponse resp = AdbHelper.readAdbResponse(this.mChannel, false);
 
/* 202 */       if (!resp.okay) {
/* 203 */         Log.w("ddms", "Got unhappy response from ADB sync req: " + resp.message);
/* 204 */         this.mChannel.close();
/* 205 */         this.mChannel = null;
/* 206 */         return false;
       }
     } catch (TimeoutException e) {
/* 209 */       if (this.mChannel != null) {
         try {
/* 211 */           this.mChannel.close();
         }
         catch (IOException e2) {
         }
/* 215 */         this.mChannel = null;
       }
 
/* 218 */       throw e;
     } catch (IOException e) {
/* 220 */       if (this.mChannel != null) {
         try {
/* 222 */           this.mChannel.close();
         }
         catch (IOException e2) {
         }
/* 226 */         this.mChannel = null;
       }
 
/* 229 */       throw e;
     }
 
/* 232 */     return true;
   }
 
   public void close()
   {
/* 239 */     if (this.mChannel != null) {
       try {
/* 241 */         this.mChannel.close();
       }
       catch (IOException e) {
       }
/* 245 */       this.mChannel = null;
     }
   }
 
   public static ISyncProgressMonitor getNullProgressMonitor()
   {
/* 255 */     return sNullSyncProgressMonitor;
   }
 
   public void pull(FileListingService.FileEntry[] entries, String localPath, ISyncProgressMonitor monitor)
     throws SyncException, IOException, TimeoutException
   {
/* 275 */     File f = new File(localPath);
/* 276 */     if (!f.exists()) {
/* 277 */       throw new SyncException(SyncException.SyncError.NO_DIR_TARGET);
     }
/* 279 */     if (!f.isDirectory()) {
/* 280 */       throw new SyncException(SyncException.SyncError.TARGET_IS_FILE);
     }
 
/* 284 */     FileListingService fls = new FileListingService(this.mDevice);
 
/* 287 */     int total = getTotalRemoteFileSize(entries, fls);
 
/* 290 */     monitor.start(total);
 
/* 292 */     doPull(entries, localPath, fls, monitor);
 
/* 294 */     monitor.stop();
   }
 
   public void pullFile(FileListingService.FileEntry remote, String localFilename, ISyncProgressMonitor monitor)
     throws IOException, SyncException, TimeoutException
   {
/* 312 */     int total = remote.getSizeValue();
/* 313 */     monitor.start(total);
 
/* 315 */     doPullFile(remote.getFullPath(), localFilename, monitor);
 
/* 317 */     monitor.stop();
   }
 
   public void pullFile(String remoteFilepath, String localFilename, ISyncProgressMonitor monitor)
     throws TimeoutException, IOException, SyncException
   {
/* 337 */     FileStat fileStat = statFile(remoteFilepath);
/* 338 */     if (fileStat != null)
     {
/* 340 */       if (fileStat.getMode() == 0) {
/* 341 */         throw new SyncException(SyncException.SyncError.NO_REMOTE_OBJECT);
       }
     }
/* 344 */     monitor.start(0);
 
/* 347 */     doPullFile(remoteFilepath, localFilename, monitor);
 
/* 349 */     monitor.stop();
   }
 
   public void push(String[] local, FileListingService.FileEntry remote, ISyncProgressMonitor monitor)
     throws SyncException, IOException, TimeoutException
   {
/* 363 */     if (!remote.isDirectory()) {
/* 364 */       throw new SyncException(SyncException.SyncError.REMOTE_IS_FILE);
     }
 
/* 368 */     ArrayList files = new ArrayList();
/* 369 */     for (String path : local) {
/* 370 */       files.add(new File(path));
     }
 
/* 374 */     File[] fileArray = (File[])files.toArray(new File[files.size()]);
/* 375 */     int total = getTotalLocalFileSize(fileArray);
 
/* 377 */     monitor.start(total);
 
/* 379 */     doPush(fileArray, remote.getFullPath(), monitor);
 
/* 381 */     monitor.stop();
   }
 
   public void pushFile(String local, String remote, ISyncProgressMonitor monitor)
     throws SyncException, IOException, TimeoutException
   {
/* 396 */     File f = new File(local);
/* 397 */     if (!f.exists()) {
/* 398 */       throw new SyncException(SyncException.SyncError.NO_LOCAL_FILE);
     }
 
/* 401 */     if (f.isDirectory()) {
/* 402 */       throw new SyncException(SyncException.SyncError.LOCAL_IS_DIRECTORY);
     }
 
/* 405 */     monitor.start((int)f.length());
 
/* 407 */     doPushFile(local, remote, monitor);
 
/* 409 */     monitor.stop();
   }
 
   private int getTotalRemoteFileSize(FileListingService.FileEntry[] entries, FileListingService fls)
   {
/* 420 */     int count = 0;
/* 421 */     for (FileListingService.FileEntry e : entries) {
/* 422 */       int type = e.getType();
/* 423 */       if (type == 1)
       {
/* 425 */         FileListingService.FileEntry[] children = fls.getChildren(e, false, null);
/* 426 */         count += getTotalRemoteFileSize(children, fls) + 1;
/* 427 */       } else if (type == 0) {
/* 428 */         count += e.getSizeValue();
       }
     }
 
/* 432 */     return count;
   }
 
   private int getTotalLocalFileSize(File[] files)
   {
/* 443 */     int count = 0;
 
/* 445 */     for (File f : files) {
/* 446 */       if (f.exists()) {
/* 447 */         if (f.isDirectory())
/* 448 */           return getTotalLocalFileSize(f.listFiles()) + 1;
/* 449 */         if (f.isFile()) {
/* 450 */           count = (int)(count + f.length());
         }
       }
     }
 
/* 455 */     return count;
   }
 
   private void doPull(FileListingService.FileEntry[] entries, String localPath, FileListingService fileListingService, ISyncProgressMonitor monitor)
     throws SyncException, IOException, TimeoutException
   {
/* 473 */     for (FileListingService.FileEntry e : entries)
     {
/* 475 */       if (monitor.isCanceled()) {
/* 476 */         throw new SyncException(SyncException.SyncError.CANCELED);
       }
 
/* 480 */       int type = e.getType();
/* 481 */       if (type == 1) {
/* 482 */         monitor.startSubTask(e.getFullPath());
/* 483 */         String dest = localPath + File.separator + e.getName();
 
/* 486 */         File d = new File(dest);
/* 487 */         d.mkdir();
 
/* 491 */         FileListingService.FileEntry[] children = fileListingService.getChildren(e, true, null);
/* 492 */         doPull(children, dest, fileListingService, monitor);
/* 493 */         monitor.advance(1);
/* 494 */       } else if (type == 0) {
/* 495 */         monitor.startSubTask(e.getFullPath());
/* 496 */         String dest = localPath + File.separator + e.getName();
/* 497 */         doPullFile(e.getFullPath(), dest, monitor);
       }
     }
   }
 
   private void doPullFile(String remotePath, String localPath, ISyncProgressMonitor monitor)
     throws IOException, SyncException, TimeoutException
   {
/* 513 */     byte[] msg = null;
/* 514 */     byte[] pullResult = new byte[8];
 
/* 516 */     int timeOut = DdmPreferences.getTimeOut();
     try
     {
/* 519 */       byte[] remotePathContent = remotePath.getBytes("ISO-8859-1");
 
/* 521 */       if (remotePathContent.length > 1024) {
/* 522 */         throw new SyncException(SyncException.SyncError.REMOTE_PATH_LENGTH);
       }
 
/* 526 */       msg = createFileReq(ID_RECV, remotePathContent);
 
/* 529 */       AdbHelper.write(this.mChannel, msg, -1, timeOut);
 
/* 533 */       AdbHelper.read(this.mChannel, pullResult, -1, timeOut);
 
/* 536 */       if ((!checkResult(pullResult, ID_DATA)) && (!checkResult(pullResult, ID_DONE)))
       {
/* 538 */         throw new SyncException(SyncException.SyncError.TRANSFER_PROTOCOL_ERROR, readErrorMessage(pullResult, timeOut));
       }
     }
     catch (UnsupportedEncodingException e) {
/* 542 */       throw new SyncException(SyncException.SyncError.REMOTE_PATH_ENCODING, e);
     }
 
/* 546 */     File f = new File(localPath);
 
/* 550 */     FileOutputStream fos = null;
     try {
/* 552 */       fos = new FileOutputStream(f);
 
/* 555 */       byte[] data = new byte[65536];
       while (true)
       {
/* 560 */         if (monitor.isCanceled()) {
/* 561 */           throw new SyncException(SyncException.SyncError.CANCELED);
         }
 
/* 565 */         if (checkResult(pullResult, ID_DONE)) {
           break;
         }
/* 568 */         if (!checkResult(pullResult, ID_DATA))
         {
/* 570 */           throw new SyncException(SyncException.SyncError.TRANSFER_PROTOCOL_ERROR, readErrorMessage(pullResult, timeOut));
         }
 
/* 573 */         int length = ArrayHelper.swap32bitFromArray(pullResult, 4);
/* 574 */         if (length > 65536)
         {
/* 577 */           throw new SyncException(SyncException.SyncError.BUFFER_OVERRUN);
         }
 
/* 581 */         AdbHelper.read(this.mChannel, data, length, timeOut);
 
/* 584 */         AdbHelper.read(this.mChannel, pullResult, -1, timeOut);
 
/* 587 */         fos.write(data, 0, length);
 
/* 589 */         monitor.advance(length);
       }
 
/* 592 */       fos.flush();
     } catch (IOException e) {
/* 594 */       Log.e("ddms", String.format("Failed to open local file %s for writing, Reason: %s", new Object[] { f.getAbsolutePath(), e.toString() }));
 
/* 596 */       throw new SyncException(SyncException.SyncError.FILE_WRITE_ERROR);
     } finally {
/* 598 */       if (fos != null)
/* 599 */         fos.close();
     }
   }
 
   private void doPush(File[] fileArray, String remotePath, ISyncProgressMonitor monitor)
     throws SyncException, IOException, TimeoutException
   {
/* 617 */     for (File f : fileArray)
     {
/* 619 */       if (monitor.isCanceled()) {
/* 620 */         throw new SyncException(SyncException.SyncError.CANCELED);
       }
/* 622 */       if (f.exists())
/* 623 */         if (f.isDirectory())
         {
/* 625 */           String dest = remotePath + "/" + f.getName();
/* 626 */           monitor.startSubTask(dest);
/* 627 */           doPush(f.listFiles(), dest, monitor);
 
/* 629 */           monitor.advance(1);
/* 630 */         } else if (f.isFile())
         {
/* 632 */           String remoteFile = remotePath + "/" + f.getName();
/* 633 */           monitor.startSubTask(remoteFile);
/* 634 */           doPushFile(f.getAbsolutePath(), remoteFile, monitor);
         }
     }
   }
 
   private void doPushFile(String localPath, String remotePath, ISyncProgressMonitor monitor)
     throws SyncException, IOException, TimeoutException
   {
/* 652 */     FileInputStream fis = null;
 
/* 655 */     int timeOut = DdmPreferences.getTimeOut();
/* 656 */     File f = new File(localPath);
     try
     {
/* 659 */       byte[] remotePathContent = remotePath.getBytes("ISO-8859-1");
 
/* 661 */       if (remotePathContent.length > 1024) {
/* 662 */         throw new SyncException(SyncException.SyncError.REMOTE_PATH_LENGTH);
       }
 
/* 666 */       fis = new FileInputStream(f);
 
/* 669 */       byte[] msg = createSendFileReq(ID_SEND, remotePathContent, 420);
 
/* 673 */       AdbHelper.write(this.mChannel, msg, -1, timeOut);
 
/* 675 */       System.arraycopy(ID_DATA, 0, getBuffer(), 0, ID_DATA.length);
       while (true)
       {
/* 680 */         if (monitor.isCanceled()) {
/* 681 */           throw new SyncException(SyncException.SyncError.CANCELED);
         }
 
/* 685 */         int readCount = fis.read(getBuffer(), 8, 65536);
 
/* 687 */         if (readCount == -1)
         {
           break;
         }
 
/* 694 */         ArrayHelper.swap32bitsToArray(readCount, getBuffer(), 4);
 
/* 697 */         AdbHelper.write(this.mChannel, getBuffer(), readCount + 8, timeOut);
 
/* 700 */         monitor.advance(readCount);
       }
     } catch (UnsupportedEncodingException e) {
/* 703 */       throw new SyncException(SyncException.SyncError.REMOTE_PATH_ENCODING, e);
     }
     finally {
/* 706 */       if (fis != null) {
/* 707 */         fis.close();
       }
 
     }
 
/* 712 */     long time = f.lastModified() / 1000L;
/* 713 */     byte[] msg = createReq(ID_DONE, (int)time);
 
/* 716 */     AdbHelper.write(this.mChannel, msg, -1, timeOut);
 
/* 720 */     byte[] result = new byte[8];
/* 721 */     AdbHelper.read(this.mChannel, result, -1, timeOut);
 
/* 723 */     if (!checkResult(result, ID_OKAY))
/* 724 */       throw new SyncException(SyncException.SyncError.TRANSFER_PROTOCOL_ERROR, readErrorMessage(result, timeOut));
   }
 
   private String readErrorMessage(byte[] result, int timeOut)
     throws TimeoutException, IOException
   {
/* 739 */     if (checkResult(result, ID_FAIL)) {
/* 740 */       int len = ArrayHelper.swap32bitFromArray(result, 4);
 
/* 742 */       if (len > 0) {
/* 743 */         AdbHelper.read(this.mChannel, getBuffer(), len, timeOut);
 
/* 745 */         String message = new String(getBuffer(), 0, len);
/* 746 */         Log.e("ddms", "transfer error: " + message);
 
/* 748 */         return message;
       }
     }
 
/* 752 */     return null;
   }
 
   public FileStat statFile(String path)
     throws TimeoutException, IOException
   {
/* 766 */     byte[] msg = createFileReq(ID_STAT, path);
 
/* 768 */     AdbHelper.write(this.mChannel, msg, -1, DdmPreferences.getTimeOut());
 
/* 772 */     byte[] statResult = new byte[16];
/* 773 */     AdbHelper.read(this.mChannel, statResult, -1, DdmPreferences.getTimeOut());
 
/* 776 */     if (!checkResult(statResult, ID_STAT)) {
/* 777 */       return null;
     }
 
/* 780 */     int mode = ArrayHelper.swap32bitFromArray(statResult, 4);
/* 781 */     int size = ArrayHelper.swap32bitFromArray(statResult, 8);
/* 782 */     int lastModifiedSecs = ArrayHelper.swap32bitFromArray(statResult, 12);
/* 783 */     return new FileStat(mode, size, lastModifiedSecs);
   }
 
   private static byte[] createReq(byte[] command, int value)
   {
/* 793 */     byte[] array = new byte[8];
 
/* 795 */     System.arraycopy(command, 0, array, 0, 4);
/* 796 */     ArrayHelper.swap32bitsToArray(value, array, 4);
 
/* 798 */     return array;
   }
 
   private static byte[] createFileReq(byte[] command, String path)
   {
/* 808 */     byte[] pathContent = null;
     try {
/* 810 */       pathContent = path.getBytes("ISO-8859-1");
     } catch (UnsupportedEncodingException e) {
/* 812 */       return null;
     }
 
/* 815 */     return createFileReq(command, pathContent);
   }
 
   private static byte[] createFileReq(byte[] command, byte[] path)
   {
/* 827 */     byte[] array = new byte[8 + path.length];
 
/* 829 */     System.arraycopy(command, 0, array, 0, 4);
/* 830 */     ArrayHelper.swap32bitsToArray(path.length, array, 4);
/* 831 */     System.arraycopy(path, 0, array, 8, path.length);
 
/* 833 */     return array;
   }
 
   private static byte[] createSendFileReq(byte[] command, byte[] path, int mode)
   {
/* 838 */     String modeStr = "," + (mode & 0x1FF);
/* 839 */     byte[] modeContent = null;
     try {
/* 841 */       modeContent = modeStr.getBytes("ISO-8859-1");
     } catch (UnsupportedEncodingException e) {
/* 843 */       return null;
     }
 
/* 846 */     byte[] array = new byte[8 + path.length + modeContent.length];
 
/* 848 */     System.arraycopy(command, 0, array, 0, 4);
/* 849 */     ArrayHelper.swap32bitsToArray(path.length + modeContent.length, array, 4);
/* 850 */     System.arraycopy(path, 0, array, 8, path.length);
/* 851 */     System.arraycopy(modeContent, 0, array, 8 + path.length, modeContent.length);
 
/* 853 */     return array;
   }
 
   private static boolean checkResult(byte[] result, byte[] code)
   {
/* 865 */     return (result[0] == code[0]) && (result[1] == code[1]) && (result[2] == code[2]) && (result[3] == code[3]);
   }
 
   private static int getFileType(int mode)
   {
/* 873 */     if ((mode & 0xC000) == 49152) {
/* 874 */       return 6;
     }
 
/* 877 */     if ((mode & 0xA000) == 40960) {
/* 878 */       return 5;
     }
 
/* 881 */     if ((mode & 0x8000) == 32768) {
/* 882 */       return 0;
     }
 
/* 885 */     if ((mode & 0x6000) == 24576) {
/* 886 */       return 3;
     }
 
/* 889 */     if ((mode & 0x4000) == 16384) {
/* 890 */       return 1;
     }
 
/* 893 */     if ((mode & 0x2000) == 8192) {
/* 894 */       return 4;
     }
 
/* 897 */     if ((mode & 0x1000) == 4096) {
/* 898 */       return 7;
     }
 
/* 901 */     return 8;
   }
 
   private byte[] getBuffer()
   {
/* 909 */     if (this.mBuffer == null)
     {
/* 912 */       this.mBuffer = new byte[65544];
     }
/* 914 */     return this.mBuffer;
   }
 
   private static class NullSyncProgressMonitor
     implements SyncService.ISyncProgressMonitor
   {
     public void advance(int work)
     {
     }
 
     public boolean isCanceled()
     {
/* 148 */       return false;
     }
 
     public void start(int totalWork)
     {
     }
 
     public void startSubTask(String name)
     {
     }
 
     public void stop()
     {
     }
   }
 
   public static class FileStat
   {
     private final int myMode;
     private final int mySize;
     private final Date myLastModified;
 
     public FileStat(int mode, int size, int lastModifiedSecs)
     {
/* 121 */       this.myMode = mode;
/* 122 */       this.mySize = size;
/* 123 */       this.myLastModified = new Date(lastModifiedSecs * 1000L);
     }
 
     public int getMode() {
/* 127 */       return this.myMode;
     }
 
     public int getSize() {
/* 131 */       return this.mySize;
     }
 
     public Date getLastModified() {
/* 135 */       return this.myLastModified;
     }
   }
 
   public static abstract interface ISyncProgressMonitor
   {
     public abstract void start(int paramInt);
 
     public abstract void stop();
 
     public abstract boolean isCanceled();
 
     public abstract void startSubTask(String paramString);
 
     public abstract void advance(int paramInt);
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.SyncService
 * JD-Core Version:    0.6.2
 */