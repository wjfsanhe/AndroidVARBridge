 package com.android.ddmlib.log;
 
 import com.android.ddmlib.IDevice;
 import com.android.ddmlib.Log;
 import com.android.ddmlib.MultiLineReceiver;
 import com.android.ddmlib.utils.ArrayHelper;
 import com.google.common.base.Charsets;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.FileReader;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.Map;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.TreeMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public final class EventLogParser
 {
   private static final String EVENT_TAG_MAP_FILE = "/system/etc/event-log-tags";
   private static final int EVENT_TYPE_INT = 0;
   private static final int EVENT_TYPE_LONG = 1;
   private static final int EVENT_TYPE_STRING = 2;
   private static final int EVENT_TYPE_LIST = 3;
/*  59 */   private static final Pattern PATTERN_SIMPLE_TAG = Pattern.compile("^(\\d+)\\s+([A-Za-z0-9_]+)\\s*$");
 
/*  61 */   private static final Pattern PATTERN_TAG_WITH_DESC = Pattern.compile("^(\\d+)\\s+([A-Za-z0-9_]+)\\s*(.*)\\s*$");
 
/*  63 */   private static final Pattern PATTERN_DESCRIPTION = Pattern.compile("\\(([A-Za-z0-9_\\s]+)\\|(\\d+)(\\|\\d+){0,1}\\)");
 
/*  66 */   private static final Pattern TEXT_LOG_LINE = Pattern.compile("(\\d\\d)-(\\d\\d)\\s(\\d\\d):(\\d\\d):(\\d\\d).(\\d{3})\\s+I/([a-zA-Z0-9_]+)\\s*\\(\\s*(\\d+)\\):\\s+(.*)");
 
/*  69 */   private final TreeMap<Integer, String> mTagMap = new TreeMap();
 
/*  71 */   private final TreeMap<Integer, EventValueDescription[]> mValueDescriptionMap = new TreeMap();
 
   public boolean init(IDevice device)
   {
     try
     {
/*  88 */       device.executeShellCommand("cat /system/etc/event-log-tags", new MultiLineReceiver()
       {
         public void processNewLines(String[] lines)
         {
/*  92 */           for (String line : lines)
/*  93 */             EventLogParser.this.processTagLine(line);
         }
 
         public boolean isCancelled()
         {
/*  98 */           return false;
         }
       });
     }
     catch (Exception e) {
/* 103 */       return false;
     }
 
/* 106 */     return true;
   }
 
   public boolean init(String[] tagFileContent)
   {
/* 115 */     for (String line : tagFileContent) {
/* 116 */       processTagLine(line);
     }
/* 118 */     return true;
   }
 
   public boolean init(String filePath)
   {
/* 127 */     BufferedReader reader = null;
     try {
/* 129 */       reader = new BufferedReader(new FileReader(filePath));
 
/* 131 */       String line = null;
       do {
/* 133 */         line = reader.readLine();
/* 134 */         if (line != null)
/* 135 */           processTagLine(line);
       }
/* 137 */       while (line != null);
 
/* 139 */       return true;
     }
     catch (IOException e)
     {
       boolean bool;
/* 141 */       return false;
     } finally {
       try {
/* 144 */         if (reader != null)
/* 145 */           reader.close();
       }
       catch (IOException e)
       {
       }
     }
   }
 
   private void processTagLine(String line)
   {
/* 159 */     if ((!line.isEmpty()) && (line.charAt(0) != '#')) {
/* 160 */       Matcher m = PATTERN_TAG_WITH_DESC.matcher(line);
/* 161 */       if (m.matches()) {
         try {
/* 163 */           int value = Integer.parseInt(m.group(1));
/* 164 */           String name = m.group(2);
/* 165 */           if ((name != null) && (this.mTagMap.get(Integer.valueOf(value)) == null)) {
/* 166 */             this.mTagMap.put(Integer.valueOf(value), name);
           }
 
/* 173 */           if (value == 20001) {
/* 174 */             this.mValueDescriptionMap.put(Integer.valueOf(value), GcEventContainer.getValueDescriptions());
           }
           else
           {
/* 178 */             String description = m.group(3);
/* 179 */             if ((description != null) && (!description.isEmpty())) {
/* 180 */               EventValueDescription[] desc = processDescription(description);
 
/* 183 */               if (desc != null)
/* 184 */                 this.mValueDescriptionMap.put(Integer.valueOf(value), desc);
             }
           }
         }
         catch (NumberFormatException e) {
         }
       }
       else {
/* 192 */         m = PATTERN_SIMPLE_TAG.matcher(line);
/* 193 */         if (m.matches()) {
/* 194 */           int value = Integer.parseInt(m.group(1));
/* 195 */           String name = m.group(2);
/* 196 */           if ((name != null) && (this.mTagMap.get(Integer.valueOf(value)) == null))
/* 197 */             this.mTagMap.put(Integer.valueOf(value), name);
         }
       }
     }
   }
 
   private EventValueDescription[] processDescription(String description)
   {
/* 205 */     String[] descriptions = description.split("\\s*,\\s*");
 
/* 207 */     ArrayList list = new ArrayList();
 
/* 209 */     for (String desc : descriptions) {
/* 210 */       Matcher m = PATTERN_DESCRIPTION.matcher(desc);
/* 211 */       if (m.matches())
         try {
/* 213 */           String name = m.group(1);
 
/* 215 */           String typeString = m.group(2);
/* 216 */           int typeValue = Integer.parseInt(typeString);
/* 217 */           EventContainer.EventValueType eventValueType = EventContainer.EventValueType.getEventValueType(typeValue);
/* 218 */           if (eventValueType == null);
/* 223 */           typeString = m.group(3);
/* 224 */           if ((typeString != null) && (!typeString.isEmpty()))
           {
/* 226 */             typeString = typeString.substring(1);
 
/* 228 */             typeValue = Integer.parseInt(typeString);
/* 229 */             EventValueDescription.ValueType valueType = EventValueDescription.ValueType.getValueType(typeValue);
 
/* 231 */             list.add(new EventValueDescription(name, eventValueType, valueType));
           } else {
/* 233 */             list.add(new EventValueDescription(name, eventValueType));
           }
         }
         catch (NumberFormatException nfe)
         {
         }
         catch (InvalidValueTypeException e)
         {
         }
       else {
/* 243 */         Log.e("EventLogParser", String.format("Can't parse %1$s", new Object[] { description }));
       }
 
     }
 
/* 248 */     if (list.isEmpty()) {
/* 249 */       return null;
     }
 
/* 252 */     return (EventValueDescription[])list.toArray(new EventValueDescription[list.size()]);
   }
 
   public EventContainer parse(LogReceiver.LogEntry entry)
   {
/* 257 */     if (entry.len < 4) {
/* 258 */       return null;
     }
 
/* 261 */     int inOffset = 0;
 
/* 263 */     int tagValue = ArrayHelper.swap32bitFromArray(entry.data, inOffset);
/* 264 */     inOffset += 4;
 
/* 266 */     String tag = (String)this.mTagMap.get(Integer.valueOf(tagValue));
/* 267 */     if (tag == null) {
/* 268 */       Log.e("EventLogParser", String.format("unknown tag number: %1$d", new Object[] { Integer.valueOf(tagValue) }));
     }
 
/* 271 */     ArrayList list = new ArrayList();
/* 272 */     if (parseBinaryEvent(entry.data, inOffset, list) == -1)
/* 273 */       return null;

     Object data;
/* 277 */     if (list.size() == 1)
/* 278 */       data = list.get(0);
     else {
/* 280 */       data = list.toArray();
     }
 
/* 283 */     EventContainer event = null;
/* 284 */     if (tagValue == 20001)
/* 285 */       event = new GcEventContainer(entry, tagValue, data);
     else {
/* 287 */       event = new EventContainer(entry, tagValue, data);
     }
 
/* 290 */     return event;
   }
 
   public EventContainer parse(String textLogLine)
   {
/* 300 */     if (textLogLine.isEmpty()) {
/* 301 */       return null;
     }
 
/* 305 */     Matcher m = TEXT_LOG_LINE.matcher(textLogLine);
/* 306 */     if (m.matches()) {
       try {
/* 308 */         int month = Integer.parseInt(m.group(1));
/* 309 */         int day = Integer.parseInt(m.group(2));
/* 310 */         int hours = Integer.parseInt(m.group(3));
/* 311 */         int minutes = Integer.parseInt(m.group(4));
/* 312 */         int seconds = Integer.parseInt(m.group(5));
/* 313 */         int milliseconds = Integer.parseInt(m.group(6));
 
/* 316 */         Calendar cal = Calendar.getInstance();
/* 317 */         cal.set(cal.get(1), month - 1, day, hours, minutes, seconds);
/* 318 */         int sec = (int)Math.floor(cal.getTimeInMillis() / 1000L);
/* 319 */         int nsec = milliseconds * 1000000;
 
/* 321 */         String tag = m.group(7);
 
/* 324 */         int tagValue = -1;
/* 325 */         Set<Map.Entry<Integer, String>> tagSet = this.mTagMap.entrySet();
/* 326 */         for (Map.Entry<Integer, String> entry : tagSet) {
/* 327 */           if (tag.equals(entry.getValue())) {
/* 328 */             tagValue = ((Integer)entry.getKey()).intValue();
/* 329 */             break;
           }
         }
 
/* 333 */         if (tagValue == -1) {
/* 334 */           return null;
         }
 
/* 337 */         int pid = Integer.parseInt(m.group(8));
 
/* 339 */         Object data = parseTextData(m.group(9), tagValue);
/* 340 */         if (data == null) {
/* 341 */           return null;
         }
 
/* 345 */         EventContainer event = null;
/* 346 */         if (tagValue == 20001) {
/* 347 */           event = new GcEventContainer(tagValue, pid, -1, sec, nsec, data);
         }
/* 349 */         return new EventContainer(tagValue, pid, -1, sec, nsec, data);
       }
       catch (NumberFormatException e)
       {
/* 354 */         return null;
       }
     }
 
/* 358 */     return null;
   }
 
   public Map<Integer, String> getTagMap() {
/* 362 */     return this.mTagMap;
   }
 
   public Map<Integer, EventValueDescription[]> getEventInfoMap() {
/* 366 */     return this.mValueDescriptionMap;
   }
 
   private static int parseBinaryEvent(byte[] eventData, int dataOffset, ArrayList<Object> list)
   {
/* 382 */     if (eventData.length - dataOffset < 1) {
/* 383 */       return -1;
     }
/* 385 */     int offset = dataOffset;
 
/* 387 */     int type = eventData[(offset++)];
 
/* 391 */     switch (type)
     {
     case 0:
/* 395 */       if (eventData.length - offset < 4)
/* 396 */         return -1;
/* 397 */       int ival = ArrayHelper.swap32bitFromArray(eventData, offset);
/* 398 */       offset += 4;
 
/* 400 */       list.add(Integer.valueOf(ival));
 
/* 402 */       break;
     case 1:
/* 406 */       if (eventData.length - offset < 8)
/* 407 */         return -1;
/* 408 */       long lval = ArrayHelper.swap64bitFromArray(eventData, offset);
/* 409 */       offset += 8;
 
/* 411 */       list.add(Long.valueOf(lval));
 
/* 413 */       break;
     case 2:
/* 417 */       if (eventData.length - offset < 4)
/* 418 */         return -1;
/* 419 */       int strLen = ArrayHelper.swap32bitFromArray(eventData, offset);
/* 420 */       offset += 4;
 
/* 422 */       if (eventData.length - offset < strLen) {
/* 423 */         return -1;
       }
 
/* 426 */       String str = new String(eventData, offset, strLen, Charsets.UTF_8);
/* 427 */       list.add(str);
/* 428 */       offset += strLen;
/* 429 */       break;
     case 3:
/* 433 */       if (eventData.length - offset < 1) {
/* 434 */         return -1;
       }
/* 436 */       int count = eventData[(offset++)];
 
/* 439 */       ArrayList subList = new ArrayList();
/* 440 */       for (int i = 0; i < count; i++) {
/* 441 */         int result = parseBinaryEvent(eventData, offset, subList);
/* 442 */         if (result == -1) {
/* 443 */           return result;
         }
 
/* 446 */         offset += result;
       }
 
/* 449 */       list.add(subList.toArray());
 
/* 451 */       break;
     default:
/* 453 */       Log.e("EventLogParser", String.format("Unknown binary event type %1$d", new Object[] { Integer.valueOf(type) }));
 
/* 455 */       return -1;
     }
 
/* 458 */     return offset - dataOffset;
   }
 
   private Object parseTextData(String data, int tagValue)
   {
/* 463 */     EventValueDescription[] desc = (EventValueDescription[])this.mValueDescriptionMap.get(Integer.valueOf(tagValue));
 
/* 465 */     if (desc == null)
     {
/* 467 */       return null;
     }
 
/* 470 */     if (desc.length == 1)
/* 471 */       return getObjectFromString(data, desc[0].getEventValueType());
/* 472 */     if ((data.startsWith("[")) && (data.endsWith("]"))) {
/* 473 */       data = data.substring(1, data.length() - 1);
 
/* 476 */       String[] values = data.split(",");
 
/* 478 */       if (tagValue == 20001)
       {
/* 480 */         Object[] objects = new Object[2];
 
/* 482 */         objects[0] = getObjectFromString(values[0], EventContainer.EventValueType.LONG);
/* 483 */         objects[1] = getObjectFromString(values[1], EventContainer.EventValueType.LONG);
 
/* 485 */         return objects;
       }
 
/* 488 */       if (values.length != desc.length) {
/* 489 */         return null;
       }
 
/* 492 */       Object[] objects = new Object[values.length];
 
/* 494 */       for (int i = 0; i < desc.length; i++) {
/* 495 */         Object obj = getObjectFromString(values[i], desc[i].getEventValueType());
/* 496 */         if (obj == null) {
/* 497 */           return null;
         }
/* 499 */         objects[i] = obj;
       }
 
/* 502 */       return objects;
     }
 
/* 506 */     return null;
   }
 
   private Object getObjectFromString(String value, EventContainer.EventValueType type)
   {
     try {
/* 512 */       switch (type.ordinal()) {
       case 1:
/* 514 */         return Integer.valueOf(value);
       case 2:
/* 516 */         return Long.valueOf(value);
       case 3:
/* 518 */         return value;
       }
     }
     catch (NumberFormatException e)
     {
     }
/* 524 */     return null;
   }
 
   public void saveTags(String filePath)
     throws IOException
   {
/* 533 */     File destFile = new File(filePath);
/* 534 */     destFile.createNewFile();
/* 535 */     FileOutputStream fos = null;
     try
     {
/* 539 */       fos = new FileOutputStream(destFile);
 
/* 541 */       for (Integer key : this.mTagMap.keySet())
       {
/* 543 */         String tagName = (String)this.mTagMap.get(key);
 
/* 546 */         EventValueDescription[] descriptors = (EventValueDescription[])this.mValueDescriptionMap.get(key);
 
/* 548 */         String line = null;
/* 549 */         if (descriptors != null) {
/* 550 */           StringBuilder sb = new StringBuilder();
/* 551 */           sb.append(String.format("%1$d %2$s", new Object[] { key, tagName }));
/* 552 */           boolean first = true;
/* 553 */           for (EventValueDescription evd : descriptors) {
/* 554 */             if (first) {
/* 555 */               sb.append(" (");
/* 556 */               first = false;
             } else {
/* 558 */               sb.append(",(");
             }
/* 560 */             sb.append(evd.getName());
/* 561 */             sb.append("|");
/* 562 */             sb.append(evd.getEventValueType().getValue());
/* 563 */             sb.append("|");
/* 564 */             sb.append(evd.getValueType().getValue());
/* 565 */             sb.append("|)");
           }
/* 567 */           sb.append("\n");
 
/* 569 */           line = sb.toString();
         } else {
/* 571 */           line = String.format("%1$d %2$s\n", new Object[] { key, tagName });
         }
 
/* 574 */         byte[] buffer = line.getBytes();
/* 575 */         fos.write(buffer);
       }
     } finally {
/* 578 */       if (fos != null)
/* 579 */         fos.close();
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.log.EventLogParser
 * JD-Core Version:    0.6.2
 */