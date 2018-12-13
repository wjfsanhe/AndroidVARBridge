package com.koushikdutta.async.sample.device;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.koushikdutta.async.sample.R;
import com.koushikdutta.async.sample.network.NetworkActivity;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceMonitorActivity extends Activity {
	private static final String TAG = "VRBridgeD";
	private AndroidDebugBridgeWrapper androidDebugBridgeWrapper;
	private DeviceChangeListener deviceChangeListener;
	private static TextView mtvStatus;
	private static EditText metCommand;
	private static int count = 0;
	private static int frameCount = 0;
	private static final int DEVICE_PHASE_OFFLINE = 0;
	private static final int DEVICE_PHASE_ONLINE = 1;
	private static final int DEVICE_PHASE_CONFIG_WIFI = 2;
	private static int mDeviceState = DEVICE_PHASE_OFFLINE;
	private static IDevice mCurrentDevice;
	private static String mCurrentDeviceIPV4Addr;
	private static String mLastLine;
	private static Context mBaseContext;
	private static final String mPort = "4444";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_layout);
		//startActivity(new Intent(this,  ResumeDownloadSample.class));
		mDeviceState = DEVICE_PHASE_OFFLINE;
		mBaseContext = getBaseContext();
		initUI();
		initADBBridge();
	}

	private EditText.OnEditorActionListener EnterListenter = new EditText.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			 if (actionId == EditorInfo.IME_ACTION_DONE) {
				Log.d(TAG, "COMMAND SEND -- " + v.getText());
			 }
			 return false;
		}
	};
	public String getCurrentCursorLine(TextView editText) {
		int selectionStart = Selection.getSelectionStart(editText.getText());
		Layout layout = editText.getLayout();
		int line = -1;
		if (!(selectionStart == -1)) {
			 line = layout.getLineForOffset(selectionStart);
		}

		int start=layout.getLineStart(line);
		int end=layout.getLineEnd(line);
		String text = layout.getText().toString();
		return text.substring(start, end);
	}
	public String getLastLine(TextView editText) {
		if (editText == null) return null;
		int line = editText.getLineCount() - 2;
		if (line < 0) return null;
		Layout layout = editText.getLayout();
		if (layout.getLineCount() <= 0) return null;
		Log.d(TAG, "Line " + line +", layout count " + layout.getLineCount());
		int start=layout.getLineStart(line);
		int end=layout.getLineEnd(line);
		String text = layout.getText().toString();
		return text.substring(start, end);
	}
	private void initUI() {
		mtvStatus = (TextView) findViewById(R.id.view_status);
		metCommand = (EditText) findViewById(R.id.edit_command);
		metCommand.setOnEditorActionListener(EnterListenter);
		mtvStatus.setMovementMethod(new ScrollingMovementMethod());
		mtvStatus.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				//process(getLastLine(mtvStatus));
			}
		});
		mtvStatus.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event){
				TextView textView = (TextView) v;
				Log.d(TAG, "touch line ---- " + getCurrentCursorLine(textView));
				process(getCurrentCursorLine(textView));
				return false;
			}
		});
	}
	private static Runnable processLineRunable = new Runnable() {
		@Override
		public void run() {
			process(mLastLine);
		}
	};


	private static void process(String input) {
		if (input == null) return;
		int start = input.indexOf("[");
		int end = input.indexOf("]");
		if (end <= 0) return;
		String device = input.substring(start + 1 , end);
		if (device.contentEquals("emulator-5554")){
			Log.d(TAG, "select emulator device, do nothing !");
			return ;
		} else {
			Log.d(TAG, "input is " + input);
			if (input.contains("connect") || input.contains("online")) {
				Log.d(TAG, "found device " + device + " online !!!!");
				if (device.contains(mPort)) { //it is network device
					Log.d(TAG, "NETWORK ADB CONNECTED");
					adbForward();
				} else { //it is usb device.
					switch (mDeviceState) {
						case DEVICE_PHASE_OFFLINE:
							mDeviceState = DEVICE_PHASE_ONLINE;
							Log.d(TAG, "prepare usb connect");
							prepareADB_USB(device);
							break;
						case DEVICE_PHASE_ONLINE:
						case DEVICE_PHASE_CONFIG_WIFI:
							Log.d(TAG, "connect wlan device\n");
							connectWlan();
							break;
					}
				}
			}
			if (input.contains("disconnect")) {
				Log.d(TAG, "device is disconnect");
				mDeviceState = DEVICE_PHASE_OFFLINE;
			}
		}
	}

	/******************************************************************************
	 *
	 * 								calling process                         process (be called)
	 * 									|											|
	 * 	input stream direction          |      <--------------------------------	|
	 * 									|											|
	 *  output stream direction			|	  -------------------------------->		|
	 *  								|											|
	 *  error stream direction			|	  <--------------------------------		|
	 * 									|											|
	 *
	 * @return
	 */
	//so the point of view is
	private static int startProcessMain(String host, String port) {
		try {
			if (host == null) {
				updataIPAddr();
				while (mCurrentDeviceIPV4Addr == null) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				host = new String(mCurrentDeviceIPV4Addr);
			}
			Log.d(TAG, "Start process Main");
			final ProcessBuilder pb = new ProcessBuilder("/system/bin/adb", "-s",host + ":" + port, "shell");
			final Process process = pb.redirectErrorStream(true).start(); //merge error output to iput stream.
			pb.directory(new File("/data/local/tmp"));
			//set environment
			Map<String, String> env = pb.environment();
			env.put("CLASSPATH", "/data/local/tmp/app-debug.apk");
			Iterator it=env.keySet().iterator();
			while(it.hasNext())
			{
				String sysatt = (String)it.next();

				System.out.println("System Attribute:"+sysatt+"="+env.get(sysatt));
				Log.d (TAG, "System Attribute:"+sysatt+"="+env.get(sysatt));
			}
			OutputStream stdout = process.getOutputStream();
			InputStream stderr = process.getErrorStream();
			InputStream stdin = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stdin));
			BufferedReader err= new BufferedReader(new InputStreamReader(stderr));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdout));
			writer.write("grep\r\n");
			writer.write("ls -l\r\n");
			writer.write("export CLASSPATH=/data/local/tmp/app-debug.apk\r\n");
			writer.write("ps |grep app_process |grep -v grep | cut -c 9-15 | xargs kill -9\r\n");
			writer.write("exec app_process /system/bin com.mzj.vysor.Main &\r\n");
			writer.write("ps | grep app_process  \r\n");
			writer.flush();
			String line;
			int count=0;
			while ((line = reader.readLine()) != null)
			{
				Log.d(TAG, "[ " + line );
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	private static void adbForward() {
		Thread forkThread = new Thread("ADB FORWARD") {
			public void run() {
				try {
					mCurrentDevice.createForward(52174, 52174);
					//we will start Main process.
					new Thread(new Runnable() {
						@Override
						public void run() {
							startProcessMain(mCurrentDeviceIPV4Addr, mPort);
							Log.d(TAG, "start process main completed");
						}
					}).start();
					while (mCurrentDeviceIPV4Addr == null) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					Intent intent = new Intent(mBaseContext, NetworkActivity.class);
					intent.putExtra("addr", mCurrentDeviceIPV4Addr);
					Log.d(TAG, "start Activity with " + mCurrentDeviceIPV4Addr);
					mBaseContext.startActivity(intent);
				} catch (TimeoutException e) {
					e.printStackTrace();
					e.printStackTrace();
				} catch (AdbCommandRejectedException e) {
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		forkThread.setDaemon(true);
		forkThread.start();
	}
	private static void connectWlan() {
		final ConnectFetcher receiver = new ConnectFetcher();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Thread forkThread = new Thread("CONECT_WLAN_ADDR") {
			public void run() {

				try {
					String host = new String(mCurrentDeviceIPV4Addr + ":" + mPort);
					Log.d(TAG, "execute adb command : adb connect to " + host);
					mCurrentDevice.createConnection(host);
					//mCurrentDevice.executeShellCommand("ls", receiver, 2000L, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (AdbCommandRejectedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		forkThread.setDaemon(true);
		forkThread.start();
	}

	private static void prepareADB_USB(String device) {
		getDeviceIPAddr_USB();
	}

	private static final class ConnectFetcher extends MultiLineReceiver
	{
		public void processNewLines(String[] lines) {
			for (String line : lines) {
				Log.d(TAG, "connect process one line " + line);
			}
		}
		public boolean isCancelled()
		{
			return false;
		}
	}
	private static final class TCPIPFetcher extends MultiLineReceiver
	{
		public void processNewLines(String[] lines) {
			for (String line : lines) {
				Log.d(TAG, "tcpip process one line " + line);
				if (line.contains("restarting in TCP mode")) {
					mDeviceState = DEVICE_PHASE_CONFIG_WIFI;
				}
			}
		}
		public boolean isCancelled()
		{
			return false;
		}
	}

	private static final class WlanAddrFetcher extends MultiLineReceiver
	{
		private static final Pattern IPV4_ADDR = Pattern.compile("\\s*inet addr:((\\d{1,3}\\.){3}.\\d{1,3})\\s*(?![0-9]+$)(?![a-zA-Z]+$)[A-Za-z0-9\\W]{1,}$");
		public void processNewLines(String[] lines) {
			for (String line : lines) {
				Log.d(TAG, "start process one line " + line);
				Matcher wlanAddrMatch = IPV4_ADDR.matcher(line);
				if (wlanAddrMatch.matches()) {
					Log.d(TAG, "got ipv4 addr :" + wlanAddrMatch.group(1));
					mCurrentDeviceIPV4Addr = new String(wlanAddrMatch.group(1));
				}
			}
		}
		public boolean isCancelled()
		{
			return false;
		}
	}

	/********************************************************8
	 *
	 *
	 * get IP addr by current device .
	 *
	 */
	private static void updataIPAddr(){
		final WlanAddrFetcher receiver = new WlanAddrFetcher();
		Thread forkThread = new Thread("GET_WLAN_ADDR") {
			public void run() {
				try {
					Log.d(TAG, "execute adb command : ifconfig wlan0");
					mCurrentDevice.executeShellCommand("ifconfig wlan0", receiver, 2000L, TimeUnit.MILLISECONDS);
					Log.d(TAG, "current ipv4 address is : " + mCurrentDeviceIPV4Addr);

				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (AdbCommandRejectedException e) {
					e.printStackTrace();
				} catch (ShellCommandUnresponsiveException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		forkThread.setDaemon(true);
		forkThread.start();
	}
	private static void getDeviceIPAddr_USB() {
		Log.d(TAG, "start get device IP address");
		final WlanAddrFetcher receiver = new WlanAddrFetcher();
		final TCPIPFetcher tcpipReceiver = new TCPIPFetcher();
		Thread forkThread = new Thread("GET_WLAN_ADDR") {
			public void run() {
				try {
					Log.d(TAG, "execute adb command : ifconfig wlan0");
					mCurrentDevice.executeShellCommand("ifconfig wlan0", receiver, 2000L, TimeUnit.MILLISECONDS);
					Log.d(TAG, "current ipv4 address is : " + mCurrentDeviceIPV4Addr);
					mCurrentDevice.executeTCPIPCommand(mPort, tcpipReceiver,2000L, TimeUnit.MILLISECONDS);

				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (AdbCommandRejectedException e) {
					e.printStackTrace();
				} catch (ShellCommandUnresponsiveException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		forkThread.setDaemon(true);
		forkThread.start();
	}

	public static boolean Isipv4(String ipv4) {
		if (ipv4 == null || ipv4.length() == 0) {
			return false;//字符串为空或者空串
		}
		String[] parts = ipv4.split("\\.");
		//因为java doc里已经说明, split的参数是reg, 即正则表达式, 如果用"|"分割, 则需使用"\\|"
		if (parts.length != 4) {
			return false;
			//分割开的数组根本就不是4个数字
		}
		for (int i = 0; i < parts.length; i++) {
			try {
				int n = Integer.parseInt(parts[i]);
				if (n < 0 || n > 255) {
					return false;//数字不在正确范围内
				}
			} catch (NumberFormatException e) {
				return false;//转换数字不正确
			}
		}
		return true;
	}
	private static class DeviceStatusPacket {
		public String info;
		public IDevice deviceHandle;
	}

			private void initADBBridge() {
				androidDebugBridgeWrapper = new AndroidDebugBridgeWrapper();
				deviceChangeListener = new DeviceChangeListener();
				androidDebugBridgeWrapper.addDeviceChangeListener(deviceChangeListener);
				androidDebugBridgeWrapper.init(true);
			}
			private static Handler mHandler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					// Gets the image task from the incoming Message object.
					switch (msg.what) {
						case MSG_UPDATE:
							DeviceStatusPacket packet =  (DeviceStatusPacket) msg.obj;
							Log.d(TAG,"updatae " + packet.info);
							if (packet.info.contains("emulator")) {
								return ;
							}
							mCurrentDevice = packet.deviceHandle;
							mtvStatus.append(packet.info);
							mLastLine = new String(packet.info);
							removeCallbacks(processLineRunable);
							postDelayed(processLineRunable, 2000);

							if(mtvStatus.getLineHeight() * mtvStatus.getLineCount() / mtvStatus.getHeight() != frameCount) {
								frameCount = mtvStatus.getLineHeight() * mtvStatus.getLineCount() / mtvStatus.getHeight();
								mtvStatus.scrollBy(0, mtvStatus.getHeight());
							}

							break;
						default:
							break;
					}
				}
			};
			private static final int MSG_UPDATE = 1;

			private static class LOG {
				static void d(IDevice device, final String str) {
					synchronized (mHandler) {
						Message msg = new Message();
						msg.what = MSG_UPDATE;
						DeviceStatusPacket packet = new DeviceStatusPacket();
						packet.deviceHandle = device;
						packet.info = "\n" + new String(str);
						msg.obj = packet;
						mHandler.sendMessage(msg);
						Log.d(TAG, str);
					}
				}
			}
			private static class DeviceChangeListener implements AndroidDebugBridge.IDeviceChangeListener {
				private final String TAG = "VRBridgeD";
				/**
				 * Sent when the a device is connected to the {@link AndroidDebugBridge}. * <p> * This is sent from a non UI thread. * * @param device the new device.
				 */
				@Override
				public void deviceConnected(IDevice device) {
					LOG.d(device,"+++++++Device connect " + "[" + device.getSerialNumber() + "]");
				}

				/**
				 * Sent when the a device is connected to the {@link AndroidDebugBridge}. * <p> * This is sent from a non UI thread. * * @param device the new device.
				 */
				@Override
				public void deviceDisconnected(IDevice device) {
					LOG.d(device, "-------Device disconnect " + "[" +  device.getSerialNumber() + "]");
				}

				/**
				 * Sent when a device data changed, or when clients are started/terminated on the device. * <p> * This is sent from a non UI thread. * * @param device the device that was updated. * @param changeMask the mask describing what changed. It can contain any of the following * values: {@link IDevice#CHANGE_BUILD_INFO}, {@link IDevice#CHANGE_STATE}, * {@link IDevice#CHANGE_CLIENT_LIST}
				 */
				@Override
				public void deviceChanged(IDevice device, int changeMask) {
					if (device.isOnline()) {
						LOG.d(device, "+++++++Device change online " + "[" + device.getSerialNumber() + "]");
					} else {
						LOG.d(device, "-------Device change offline " + "[" +  device.getSerialNumber() + "]");
					}
				}
			}
}

