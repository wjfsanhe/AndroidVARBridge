 package com.android.ddmlib;
 
 import java.nio.ByteBuffer;
 
 public class AllocationsParser
 {
   private static String descriptorToDot(String str)
   {
/*  29 */     int array = 0;
/*  30 */     while (str.startsWith("[")) {
/*  31 */       str = str.substring(1);
/*  32 */       array++;
     }
 
/*  35 */     int len = str.length();
 
/*  38 */     if ((len >= 2) && (str.charAt(0) == 'L') && (str.charAt(len - 1) == ';')) {
/*  39 */       str = str.substring(1, len - 1);
/*  40 */       str = str.replace('/', '.');
     }
/*  43 */     else if ("C".equals(str)) {
/*  44 */       str = "char";
/*  45 */     } else if ("B".equals(str)) {
/*  46 */       str = "byte";
/*  47 */     } else if ("Z".equals(str)) {
/*  48 */       str = "boolean";
/*  49 */     } else if ("S".equals(str)) {
/*  50 */       str = "short";
/*  51 */     } else if ("I".equals(str)) {
/*  52 */       str = "int";
/*  53 */     } else if ("J".equals(str)) {
/*  54 */       str = "long";
/*  55 */     } else if ("F".equals(str)) {
/*  56 */       str = "float";
/*  57 */     } else if ("D".equals(str)) {
/*  58 */       str = "double";
     }
 
/*  63 */     for (int a = 0; a < array; a++) {
/*  64 */       str = str + "[]";
     }
 
/*  67 */     return str;
   }
 
   private static void readStringTable(ByteBuffer data, String[] strings)
   {
/*  77 */     int count = strings.length;
 
/*  80 */     for (int i = 0; i < count; i++) {
/*  81 */       int nameLen = data.getInt();
/*  82 */       String descriptor = ByteBufferUtil.getString(data, nameLen);
/*  83 */       strings[i] = descriptorToDot(descriptor);
     }
   }
 
   public static AllocationInfo[] parse(ByteBuffer data)
   {
/* 124 */     int messageHdrLen = data.get() & 0xFF;
/* 125 */     int entryHdrLen = data.get() & 0xFF;
/* 126 */     int stackFrameLen = data.get() & 0xFF;
/* 127 */     int numEntries = data.getShort() & 0xFFFF;
/* 128 */     int offsetToStrings = data.getInt();
/* 129 */     int numClassNames = data.getShort() & 0xFFFF;
/* 130 */     int numMethodNames = data.getShort() & 0xFFFF;
/* 131 */     int numFileNames = data.getShort() & 0xFFFF;
 
/* 137 */     data.position(offsetToStrings);
 
/* 139 */     String[] classNames = new String[numClassNames];
/* 140 */     String[] methodNames = new String[numMethodNames];
/* 141 */     String[] fileNames = new String[numFileNames];
 
/* 143 */     readStringTable(data, classNames);
/* 144 */     readStringTable(data, methodNames);
/* 145 */     readStringTable(data, fileNames);
 
/* 151 */     data.position(messageHdrLen);
 
/* 153 */     AllocationInfo[] allocations = new AllocationInfo[numEntries];
/* 154 */     for (int i = 0; i < numEntries; i++)
     {
/* 158 */       int totalSize = data.getInt();
/* 159 */       int threadId = data.getShort() & 0xFFFF;
/* 160 */       int classNameIndex = data.getShort() & 0xFFFF;
/* 161 */       int stackDepth = data.get() & 0xFF;
 
/* 163 */       for (int skip = 9; skip < entryHdrLen; skip++) {
/* 164 */         data.get();
       }
/* 166 */       StackTraceElement[] steArray = new StackTraceElement[stackDepth];
 
/* 171 */       for (int sti = 0; sti < stackDepth; sti++)
       {
/* 177 */         int methodClassNameIndex = data.getShort() & 0xFFFF;
/* 178 */         int methodNameIndex = data.getShort() & 0xFFFF;
/* 179 */         int methodSourceFileIndex = data.getShort() & 0xFFFF;
/* 180 */         short lineNumber = data.getShort();
 
/* 182 */         String methodClassName = classNames[methodClassNameIndex];
/* 183 */         String methodName = methodNames[methodNameIndex];
/* 184 */         String methodSourceFile = fileNames[methodSourceFileIndex];
 
/* 186 */         steArray[sti] = new StackTraceElement(methodClassName, methodName, methodSourceFile, lineNumber);
 
/* 190 */         for (int skip = 8; skip < stackFrameLen; skip++) {
/* 191 */           data.get();
         }
       }
/* 194 */       allocations[i] = new AllocationInfo(numEntries - i, classNames[classNameIndex], totalSize, (short)threadId, steArray);
     }
/* 196 */     return allocations;
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.AllocationsParser
 * JD-Core Version:    0.6.2
 */