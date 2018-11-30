 package com.android.ddmlib;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.HashMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public final class FileListingService
 {
/*  34 */   private static final Pattern sApkPattern = Pattern.compile(".*\\.apk", 2);
   private static final String PM_FULL_LISTING = "pm list packages -f";
/*  42 */   private static final Pattern sPmPattern = Pattern.compile("^package:(.+?)=(.+)$");
   public static final String DIRECTORY_DATA = "data";
   public static final String DIRECTORY_SDCARD = "sdcard";
   public static final String DIRECTORY_MNT = "mnt";
   public static final String DIRECTORY_SYSTEM = "system";
   public static final String DIRECTORY_TEMP = "tmp";
   public static final String DIRECTORY_APP = "app";
   public static final long REFRESH_RATE = 5000L;
   static final long REFRESH_TEST = 4000L;
   public static final int TYPE_FILE = 0;
   public static final int TYPE_DIRECTORY = 1;
   public static final int TYPE_DIRECTORY_LINK = 2;
   public static final int TYPE_BLOCK = 3;
   public static final int TYPE_CHARACTER = 4;
   public static final int TYPE_LINK = 5;
   public static final int TYPE_SOCKET = 6;
   public static final int TYPE_FIFO = 7;
   public static final int TYPE_OTHER = 8;
   public static final String FILE_SEPARATOR = "/";
   private static final String FILE_ROOT = "/";
/*  91 */   public static final Pattern LS_L_PATTERN = Pattern.compile("^([bcdlsp-][-r][-w][-xsS][-r][-w][-xsS][-r][-w][-xstST])\\s+(?:\\d+\\s+)?(\\S+)\\s+(\\S+)\\s+([\\d\\s,]*)\\s+(\\d{4}-\\d\\d-\\d\\d)\\s+(\\d\\d:\\d\\d)\\s+(.*)$");
 
/*  97 */   public static final Pattern LS_LD_PATTERN = Pattern.compile("d[rwx-]{9}\\s+(\\d+\\s+)?\\S+\\s+\\S+\\s+(\\d+\\s+)?[0-9-]{10}\\s+\\d{2}:\\d{2}.*$");
   private Device mDevice;
   private FileEntry mRoot;
/* 110 */   private final ArrayList<Thread> mThreadList = new ArrayList();
 
   FileListingService(Device device)
   {
/* 651 */     this.mDevice = device;
   }
 
   public FileEntry getRoot()
   {
/* 660 */     if (this.mDevice != null) {
/* 661 */       if (this.mRoot == null) {
/* 662 */         this.mRoot = new FileEntry(null, "", 1, true);
       }
 
/* 666 */       return this.mRoot;
     }
 
/* 669 */     return null;
   }
 
   public FileEntry[] getChildren(final FileEntry entry, boolean useCache, final IListingReceiver receiver)
   {
/* 701 */     if ((useCache) && (!entry.needFetch())) {
/* 702 */       return entry.getCachedChildren();
     }
 
/* 707 */     if (receiver == null) {
/* 708 */       doLs(entry);
/* 709 */       return entry.getCachedChildren();
     }
 
/* 715 */     Thread t = new Thread("ls " + entry.getFullPath())
     {
       public void run() {
/* 718 */         FileListingService.this.doLs(entry);
 
/* 720 */         receiver.setChildren(entry, entry.getCachedChildren());
 
/* 722 */         FileListingService.FileEntry[] children = entry.getCachedChildren();
/* 723 */         if ((children.length > 0) && (children[0].isApplicationPackage())) {
/* 724 */           final HashMap map = new HashMap();
 
/* 726 */           for (FileListingService.FileEntry child : children) {
/* 727 */             String path = child.getFullPath();
/* 728 */             map.put(path, child);
           }
 
/* 732 */           String command = "pm list packages -f";
           try {
/* 734 */             FileListingService.this.mDevice.executeShellCommand(command, new MultiLineReceiver()
             {
               public void processNewLines(String[] lines) {
/* 737 */                 for (String line : lines)
/* 738 */                   if (!line.isEmpty())
                   {
/* 740 */                     Matcher m = FileListingService.sPmPattern.matcher(line);
/* 741 */                     if (m.matches())
                     {
/* 743 */                       FileListingService.FileEntry entry = (FileListingService.FileEntry)map.get(m.group(1));
/* 744 */                       if (entry != null) {
/* 745 */                         entry.info = m.group(2);
/* 746 */                         receiver.refreshEntry(entry);
                       }
                     }
                   }
               }
 
               public boolean isCancelled()
               {
/* 754 */                 return false;
               }
             });
           }
           catch (Exception e)
           {
           }
 
         }
 
/* 764 */         synchronized (FileListingService.this.mThreadList)
         {
/* 766 */           FileListingService.this.mThreadList.remove(this);
 
/* 769 */           if (!FileListingService.this.mThreadList.isEmpty()) {
/* 770 */             Thread t = (Thread)FileListingService.this.mThreadList.get(0);
/* 771 */             t.start();
           }
         }
       }
     };
/* 780 */     synchronized (this.mThreadList)
     {
/* 782 */       this.mThreadList.add(t);
 
/* 785 */       if (this.mThreadList.size() == 1) {
/* 786 */         t.start();
       }
 
     }
 
/* 791 */     return null;
   }
 
   public FileEntry[] getChildrenSync(FileEntry entry)
     throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 812 */     doLsAndThrow(entry);
/* 813 */     return entry.getCachedChildren();
   }
 
   private void doLs(FileEntry entry) {
     try {
/* 818 */       doLsAndThrow(entry);
     }
     catch (Exception e)
     {
     }
   }
 
   private void doLsAndThrow(FileEntry entry) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
   {
/* 827 */     ArrayList entryList = new ArrayList();
 
/* 830 */     ArrayList linkList = new ArrayList();
     try
     {
/* 834 */       String command = "ls -l " + entry.getFullEscapedPath();
/* 835 */       if (entry.isDirectory())
       {
/* 839 */         command = command + "/";
       }
 
/* 843 */       LsReceiver receiver = new LsReceiver(entry, entryList, linkList);
 
/* 846 */       this.mDevice.executeShellCommand(command, receiver);
 
/* 849 */       receiver.finishLinks(this.mDevice, entryList);
     }
     finally {
/* 852 */       entry.fetchTime = System.currentTimeMillis();
 
/* 855 */       Collections.sort(entryList, FileEntry.sEntryComparator);
/* 856 */       entry.setChildren(entryList);
     }
   }
 
   public static abstract interface IListingReceiver
   {
     public abstract void setChildren(FileListingService.FileEntry paramFileEntry, FileListingService.FileEntry[] paramArrayOfFileEntry);
 
     public abstract void refreshEntry(FileListingService.FileEntry paramFileEntry);
   }
 
   private static class LsReceiver extends MultiLineReceiver
   {
     private ArrayList<FileListingService.FileEntry> mEntryList;
     private ArrayList<String> mLinkList;
     private FileListingService.FileEntry[] mCurrentChildren;
     private FileListingService.FileEntry mParentEntry;
 
     public LsReceiver(FileListingService.FileEntry parentEntry, ArrayList<FileListingService.FileEntry> entryList, ArrayList<String> linkList)
     {
/* 443 */       this.mParentEntry = parentEntry;
/* 444 */       this.mCurrentChildren = parentEntry.getCachedChildren();
/* 445 */       this.mEntryList = entryList;
/* 446 */       this.mLinkList = linkList;
     }
 
     public void processNewLines(String[] lines)
     {
/* 451 */       for (String line : lines)
       {
/* 453 */         if (!line.isEmpty())
         {
/* 458 */           Matcher m = FileListingService.LS_L_PATTERN.matcher(line);
/* 459 */           if (m.matches())
           {
/* 464 */             String name = m.group(7);
 
/* 467 */             String permissions = m.group(1);
/* 468 */             String owner = m.group(2);
/* 469 */             String group = m.group(3);
/* 470 */             String size = m.group(4);
/* 471 */             String date = m.group(5);
/* 472 */             String time = m.group(6);
/* 473 */             String info = null;
 
/* 476 */             int objectType = 8;
/* 477 */             switch (permissions.charAt(0)) {
             case '-':
/* 479 */               objectType = 0;
/* 480 */               break;
             case 'b':
/* 482 */               objectType = 3;
/* 483 */               break;
             case 'c':
/* 485 */               objectType = 4;
/* 486 */               break;
             case 'd':
/* 488 */               objectType = 1;
/* 489 */               break;
             case 'l':
/* 491 */               objectType = 5;
/* 492 */               break;
             case 's':
/* 494 */               objectType = 6;
/* 495 */               break;
             case 'p':
/* 497 */               objectType = 7;
             }
 
/* 503 */             if (objectType == 5) {
/* 504 */               String[] segments = name.split("\\s->\\s");
 
/* 507 */               if (segments.length == 2)
               {
/* 509 */                 name = segments[0];
 
/* 512 */                 info = segments[1];
 
/* 515 */                 String[] pathSegments = info.split("/");
/* 516 */                 if (pathSegments.length == 1)
                 {
/* 519 */                   if ("..".equals(pathSegments[0]))
                   {
/* 521 */                     objectType = 2;
                   }
 
                 }
 
               }
 
/* 530 */               info = "-> " + info;
             }
 
/* 534 */             FileListingService.FileEntry entry = getExistingEntry(name);
/* 535 */             if (entry == null) {
/* 536 */               entry = new FileListingService.FileEntry(this.mParentEntry, name, objectType, false);
             }
 
/* 540 */             entry.permissions = permissions;
/* 541 */             entry.size = size;
/* 542 */             entry.date = date;
/* 543 */             entry.time = time;
/* 544 */             entry.owner = owner;
/* 545 */             entry.group = group;
/* 546 */             if (objectType == 5) {
/* 547 */               entry.info = info;
             }
 
/* 550 */             this.mEntryList.add(entry);
           }
         }
       }
     }
 
     private FileListingService.FileEntry getExistingEntry(String name)
     {
/* 561 */       for (int i = 0; i < this.mCurrentChildren.length; i++) {
/* 562 */         FileListingService.FileEntry e = this.mCurrentChildren[i];
 
/* 566 */         if (e != null)
         {
/* 568 */           if (name.equals(e.name))
           {
/* 570 */             this.mCurrentChildren[i] = null;
 
/* 573 */             return e;
           }
         }
 
       }
 
/* 579 */       return null;
     }
 
     public boolean isCancelled()
     {
/* 584 */       return false;
     }
 
     public void finishLinks(IDevice device, ArrayList<FileListingService.FileEntry> entries)
       throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
     {
/* 594 */       final int[] nLines = { 0 };
/* 595 */       MultiLineReceiver receiver = new MultiLineReceiver()
       {
         public void processNewLines(String[] lines) {
/* 598 */           for (String line : lines) {
/* 599 */             Matcher m = FileListingService.LS_LD_PATTERN.matcher(line);
/* 600 */             if (m.matches())
/* 601 */               nLines[0] += 1;
           }
         }
 
         public boolean isCancelled()
         {
/* 608 */           return false;
         }
       };
/* 612 */       for (FileListingService.FileEntry entry : entries)
/* 613 */         if (entry.getType() == 5)
         {
/* 619 */           nLines[0] = 0;
 
/* 621 */           String command = String.format("ls -l -d %s%s", new Object[] { entry.getFullEscapedPath(), "/" });
 
/* 624 */           device.executeShellCommand(command, receiver);
 
/* 626 */           if (nLines[0] > 0)
           {
/* 628 */             entry.setType(2);
           }
         }
     }
   }
 
   public static final class FileEntry
   {
/* 119 */     private static final Pattern sEscapePattern = Pattern.compile("([\\\\()*+?\"'&#/\\s])");
 
/* 125 */     private static Comparator<FileEntry> sEntryComparator = new Comparator<FileEntry>()
     {
       public int compare(FileListingService.FileEntry o1, FileListingService.FileEntry o2) {
/* 128 */         if (((o1 instanceof FileListingService.FileEntry)) && ((o2 instanceof FileListingService.FileEntry))) {
/* 129 */           FileListingService.FileEntry fe1 = o1;
/* 130 */           FileListingService.FileEntry fe2 = o2;
/* 131 */           return fe1.name.compareTo(fe2.name);
         }
/* 133 */         return 0;
       }
/* 125 */     };
     FileEntry parent;
     String name;
     String info;
     String permissions;
     String size;
     String date;
     String time;
     String owner;
     String group;
     int type;
     boolean isAppPackage;
     boolean isRoot;
/* 154 */     long fetchTime = 0L;
 
/* 156 */     final ArrayList<FileEntry> mChildren = new ArrayList();
 
     private FileEntry(FileEntry parent, String name, int type, boolean isRoot)
     {
/* 166 */       this.parent = parent;
/* 167 */       this.name = name;
/* 168 */       this.type = type;
/* 169 */       this.isRoot = isRoot;
 
/* 171 */       checkAppPackageStatus();
     }
 
     public String getName()
     {
/* 178 */       return this.name;
     }
 
     public String getSize()
     {
/* 185 */       return this.size;
     }
 
     public int getSizeValue()
     {
/* 192 */       return Integer.parseInt(this.size);
     }
 
     public String getDate()
     {
/* 199 */       return this.date;
     }
 
     public String getTime()
     {
/* 206 */       return this.time;
     }
 
     public String getPermissions()
     {
/* 213 */       return this.permissions;
     }
 
     public String getOwner()
     {
/* 220 */       return this.owner;
     }
 
     public String getGroup()
     {
/* 227 */       return this.group;
     }
 
     public String getInfo()
     {
/* 237 */       return this.info;
     }
 
     public String getFullPath()
     {
/* 245 */       if (this.isRoot) {
/* 246 */         return "/";
       }
/* 248 */       StringBuilder pathBuilder = new StringBuilder();
/* 249 */       fillPathBuilder(pathBuilder, false);
 
/* 251 */       return pathBuilder.toString();
     }
 
     public String getFullEscapedPath()
     {
/* 260 */       StringBuilder pathBuilder = new StringBuilder();
/* 261 */       fillPathBuilder(pathBuilder, true);
 
/* 263 */       return pathBuilder.toString();
     }
 
     public String[] getPathSegments()
     {
/* 270 */       ArrayList list = new ArrayList();
/* 271 */       fillPathSegments(list);
 
/* 273 */       return (String[])list.toArray(new String[list.size()]);
     }
 
     public int getType()
     {
/* 280 */       return this.type;
     }
 
     public void setType(int type)
     {
/* 287 */       this.type = type;
     }
 
     public boolean isDirectory()
     {
/* 294 */       return (this.type == 1) || (this.type == 2);
     }
 
     public FileEntry getParent()
     {
/* 301 */       return this.parent;
     }
 
     public FileEntry[] getCachedChildren()
     {
/* 309 */       return (FileEntry[])this.mChildren.toArray(new FileEntry[this.mChildren.size()]);
     }
 
     public FileEntry findChild(String name)
     {
/* 319 */       for (FileEntry entry : this.mChildren) {
/* 320 */         if (entry.name.equals(name)) {
/* 321 */           return entry;
         }
       }
/* 324 */       return null;
     }
 
     public boolean isRoot()
     {
/* 331 */       return this.isRoot;
     }
 
     void addChild(FileEntry child) {
/* 335 */       this.mChildren.add(child);
     }
 
     void setChildren(ArrayList<FileEntry> newChildren) {
/* 339 */       this.mChildren.clear();
/* 340 */       this.mChildren.addAll(newChildren);
     }
 
     boolean needFetch() {
/* 344 */       if (this.fetchTime == 0L) {
/* 345 */         return true;
       }
/* 347 */       long current = System.currentTimeMillis();
/* 348 */       return current - this.fetchTime > 4000L;
     }
 
     public boolean isApplicationPackage()
     {
/* 356 */       return this.isAppPackage;
     }
 
     public boolean isAppFileName()
     {
/* 363 */       Matcher m = FileListingService.sApkPattern.matcher(this.name);
/* 364 */       return m.matches();
     }
 
     protected void fillPathBuilder(StringBuilder pathBuilder, boolean escapePath)
     {
/* 374 */       if (this.isRoot) {
/* 375 */         return;
       }
 
/* 378 */       if (this.parent != null) {
/* 379 */         this.parent.fillPathBuilder(pathBuilder, escapePath);
       }
/* 381 */       pathBuilder.append("/");
/* 382 */       pathBuilder.append(escapePath ? escape(this.name) : this.name);
     }
 
     protected void fillPathSegments(ArrayList<String> list)
     {
/* 390 */       if (this.isRoot) {
/* 391 */         return;
       }
 
/* 394 */       if (this.parent != null) {
/* 395 */         this.parent.fillPathSegments(list);
       }
 
/* 398 */       list.add(this.name);
     }
 
     private void checkAppPackageStatus()
     {
/* 406 */       this.isAppPackage = false;
 
/* 408 */       String[] segments = getPathSegments();
/* 409 */       if ((this.type == 0) && (segments.length == 3) && (isAppFileName()))
/* 410 */         this.isAppPackage = (("app".equals(segments[1])) && (("system".equals(segments[0])) || ("data".equals(segments[0]))));
     }
 
     public static String escape(String entryName)
     {
/* 420 */       return sEscapePattern.matcher(entryName).replaceAll("\\\\$1");
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.FileListingService
 * JD-Core Version:    0.6.2
 */