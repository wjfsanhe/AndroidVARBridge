 package com.android.ddmlib.utils;
 
 import java.util.ArrayList;
 import java.util.List;
 
 public class DebuggerPorts
 {
/* 24 */   private final List<Integer> mDebuggerPorts = new ArrayList();
 
   public DebuggerPorts(int basePort)
   {
/* 28 */     this.mDebuggerPorts.add(Integer.valueOf(basePort));
   }
 
   public int next()
   {
/* 33 */     synchronized (this.mDebuggerPorts) {
/* 34 */       if (!this.mDebuggerPorts.isEmpty()) {
/* 35 */         int port = ((Integer)this.mDebuggerPorts.get(0)).intValue();
 
/* 38 */         this.mDebuggerPorts.remove(0);
 
/* 41 */         if (this.mDebuggerPorts.isEmpty()) {
/* 42 */           this.mDebuggerPorts.add(Integer.valueOf(port + 1));
         }
 
/* 45 */         return port;
       }
     }
 
/* 49 */     return -1;
   }
 
   public void free(int port) {
/* 53 */     if (port <= 0) {
/* 54 */       return;
     }
 
/* 57 */     synchronized (this.mDebuggerPorts)
     {
/* 60 */       if (this.mDebuggerPorts.indexOf(Integer.valueOf(port)) == -1)
       {
/* 63 */         int count = this.mDebuggerPorts.size();
/* 64 */         for (int i = 0; i < count; i++)
/* 65 */           if (port < ((Integer)this.mDebuggerPorts.get(i)).intValue()) {
/* 66 */             this.mDebuggerPorts.add(i, Integer.valueOf(port));
/* 67 */             break;
           }
       }
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.utils.DebuggerPorts
 * JD-Core Version:    0.6.2
 */