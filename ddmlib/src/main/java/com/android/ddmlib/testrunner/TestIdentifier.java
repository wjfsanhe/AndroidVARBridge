 package com.android.ddmlib.testrunner;
 
 public class TestIdentifier
 {
   private final String mClassName;
   private final String mTestName;
 
   public TestIdentifier(String className, String testName)
   {
/* 34 */     if ((className == null) || (testName == null)) {
/* 35 */       throw new IllegalArgumentException("className and testName must be non-null");
     }
 
/* 38 */     this.mClassName = className;
/* 39 */     this.mTestName = testName;
   }
 
   public String getClassName()
   {
/* 46 */     return this.mClassName;
   }
 
   public String getTestName()
   {
/* 53 */     return this.mTestName;
   }
 
   public int hashCode()
   {
/* 58 */     int prime = 31;
/* 59 */     int result = 1;
/* 60 */     result = 31 * result + (this.mClassName == null ? 0 : this.mClassName.hashCode());
/* 61 */     result = 31 * result + (this.mTestName == null ? 0 : this.mTestName.hashCode());
/* 62 */     return result;
   }
 
   public boolean equals(Object obj)
   {
/* 67 */     if (this == obj)
/* 68 */       return true;
/* 69 */     if (obj == null)
/* 70 */       return false;
/* 71 */     if (getClass() != obj.getClass())
/* 72 */       return false;
/* 73 */     TestIdentifier other = (TestIdentifier)obj;
/* 74 */     if (this.mClassName == null) {
/* 75 */       if (other.mClassName != null)
/* 76 */         return false;
/* 77 */     } else if (!this.mClassName.equals(other.mClassName))
/* 78 */       return false;
/* 79 */     if (this.mTestName == null) {
/* 80 */       if (other.mTestName != null)
/* 81 */         return false;
/* 82 */     } else if (!this.mTestName.equals(other.mTestName))
/* 83 */       return false;
/* 84 */     return true;
   }
 
   public String toString()
   {
/* 89 */     return String.format("%s#%s", new Object[] { getClassName(), getTestName() });
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.testrunner.TestIdentifier
 * JD-Core Version:    0.6.2
 */