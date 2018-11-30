package com.koushikdutta.async.sample.device;

import android.util.Log;

import com.android.ddmlib.AndroidDebugBridge;

public class AndroidDebugBridgeWrapper {
	/**
	 * android bridge
	 */
	private final String TAG = "VRBridgeD";
	private AndroidDebugBridge mAdbBridge;

	public AndroidDebugBridgeWrapper() {

	}

	public void init(boolean clientSupport) {
		AndroidDebugBridge.init(clientSupport);
		mAdbBridge = AndroidDebugBridge.createBridge("/system/bin/adb", false);
		Log.d(TAG, "mAdbBridge = " + mAdbBridge);
		//mAdbBridge.restart();
		if (mAdbBridge != null)
		Log.d(TAG, "mAdbBridge have " + mAdbBridge.getDevices().length + "devices ") ;
	}

	/**
	 * 注册设备监听器 * * @param listener 监听器
	 */
	public void addDeviceChangeListener(AndroidDebugBridge.IDeviceChangeListener listener) {
		AndroidDebugBridge.addDeviceChangeListener(listener);
	}

	/**
	 * 移除监听器 * * @param listener 监听器
	 */
	public void removeDeviceChangeListener(AndroidDebugBridge.IDeviceChangeListener listener) {
		AndroidDebugBridge.removeDeviceChangeListener(listener);
	}

	public void terminate() {
		AndroidDebugBridge.terminate();
	}

	public void disconnectBridge() {
		AndroidDebugBridge.disconnectBridge();
	}
}