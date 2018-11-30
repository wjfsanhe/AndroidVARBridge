 package com.android.ddmlib;
 
 public class SyncException extends CanceledException
 {
   private static final long serialVersionUID = 1L;
   private final SyncError mError;
 
   public SyncException(SyncError error)
   {
/* 72 */     super(error.getMessage());
/* 73 */     this.mError = error;
   }
 
   public SyncException(SyncError error, String message) {
/* 77 */     super(message);
/* 78 */     this.mError = error;
   }
 
   public SyncException(SyncError error, Throwable cause) {
/* 82 */     super(error.getMessage(), cause);
/* 83 */     this.mError = error;
   }
 
   public SyncError getErrorCode() {
/* 87 */     return this.mError;
   }
 
   public boolean wasCanceled()
   {
/* 95 */     return this.mError == SyncError.CANCELED;
   }
 
   public static enum SyncError
   {
/* 32 */     CANCELED("Operation was canceled by the user."), 
 
/* 34 */     TRANSFER_PROTOCOL_ERROR("Adb Transfer Protocol Error."), 
 
/* 36 */     NO_REMOTE_OBJECT("Remote object doesn't exist!"), 
 
/* 38 */     TARGET_IS_FILE("Target object is a file."), 
 
/* 40 */     NO_DIR_TARGET("Target directory doesn't exist."), 
 
/* 42 */     REMOTE_PATH_ENCODING("Remote Path encoding is not supported."), 
 
/* 44 */     REMOTE_PATH_LENGTH("Remote path is too long."), 
 
/* 46 */     FILE_READ_ERROR("Reading local file failed!"), 
 
/* 48 */     FILE_WRITE_ERROR("Writing local file failed!"), 
 
/* 50 */     LOCAL_IS_DIRECTORY("Local path is a directory."), 
 
/* 52 */     NO_LOCAL_FILE("Local path doesn't exist."), 
 
/* 54 */     REMOTE_IS_FILE("Remote path is a file."), 
 
/* 56 */     BUFFER_OVERRUN("Receiving too much data.");
 
     private final String mMessage;
 
     private SyncError(String message) {
/* 61 */       this.mMessage = message;
     }
 
     public String getMessage() {
/* 65 */       return this.mMessage;
     }
   }
 }

/* Location:           /disk/B/share/ddmlib/ddmlib-24.5.0-beta2.jar
 * Qualified Name:     com.android.ddmlib.SyncException
 * JD-Core Version:    0.6.2
 */