package com.koushikdutta.async.sample.network;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.sample.R;
import com.koushikdutta.async.sample.floatUtil.FloatWindow;
import com.koushikdutta.async.sample.floatUtil.MoveType;
import com.koushikdutta.async.sample.floatUtil.Screen;
import com.koushikdutta.async.sample.render.RenderManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class NetworkActivity extends Activity implements  SurfaceHolder.Callback{

	protected static final String PROTOCOL_HTTP = "http://";
	protected static final String PROTOCOL_HTTPS = "https://";
	protected static final int LIGHTGREEN = Color.parseColor("#00FF66");
	protected static final int LIGHTRED = Color.parseColor("#FF3300");
	protected static final int YELLOW = Color.parseColor("#FFFF00");
	protected static final int LIGHTBLUE = Color.parseColor("#99CCFF");
	private static final String LOG_TAG = NetworkActivity.class.getSimpleName();
	private final String ADB_TAG = "VRBridgeD";
	private static final int MENU_USE_HTTPS = 0;
	private static final int MENU_CLEAR_VIEW = 1;
	private static final int MENU_LOGGING_VERBOSITY = 2;
	private static final int MENU_ENABLE_LOGGING = 3;
	private static String DEFAULT_URL = "192.168.1.109:52174/h264";
	protected static String PROTOCOL = PROTOCOL_HTTP;
	private FileOutputStream fileMirror = null;
	private static boolean mStreamRecord = false;
	public LinearLayout customFieldsLayout;
	private Button runButton;
	private Button cancelButton;
	private String remoteIPV4Addr;
	private Button usbButton;
	SurfaceHolder mSurfaceHolder;
	SurfaceView surfaceView;
	int surfaceViewWidth;
	int surfaceViewHeight;
	boolean mPaired = false;
	Handler mHandler = new Handler();

	private EditText urlEditText, headersEditText, bodyEditText;
	private boolean isLongStream = false;
	private RenderManager mRenderManager;
	private StreamBuffer mStreamBuffer = StreamBuffer.getInstance();
	private Future<File> mFutureShot = null;
	private Future<Integer> mFutureMirror = null;
	private WebSocket mInputChannel = null;
	private RectF mMirrorScreenRect;

	private enum RequestType{
		SCREENSHOT,
		MIRROR
	}
	public static class configuration {
		public static int LISTEN_PORT = 52174;
		public static int ScreenWIDTH = 2160;
		public static int ScreenHEIGHT = 2160;
		public static int FRAME_RATE = 30;// 帧率
		public static int IFRAME_INTERVAL = 1;//  I帧间隔
		public static int TIMEOUT_US = 10 * 1000;
		public static int BITRATE = 1000 * 1000;//码率
		public static String MIME_TYPE = "video/avc"; // H.264 编码
		public static int REMOTE_WIDTH;
		public static int REMOTE_HEIGHT;
		public static void dump() {
			Log.d(LOG_TAG, "--------------------------------------------------------");
			Log.d(LOG_TAG, "LISTEN_PORT : " + configuration.LISTEN_PORT);
			Log.d(LOG_TAG, "ScreenWIDTH : " + configuration.ScreenWIDTH);
			Log.d(LOG_TAG, "ScreenHEIGHT : " + configuration.ScreenHEIGHT);
			Log.d(LOG_TAG, "ScreenWIDTH remote : " + configuration.REMOTE_WIDTH);
			Log.d(LOG_TAG, "ScreenHEIGHT remote: " + configuration.REMOTE_HEIGHT);
			Log.d(LOG_TAG, "FRAME_RATE : " + configuration.FRAME_RATE);
			Log.d(LOG_TAG, "IFRAME_INTERVAL : " + configuration.IFRAME_INTERVAL);
			Log.d(LOG_TAG, "BITRATE : " + configuration.BITRATE);
			Log.d(LOG_TAG, "TIMEOUT_US : " + configuration.TIMEOUT_US);
			Log.d(LOG_TAG, "MIME_TYPE : " + configuration.MIME_TYPE);
			Log.d(LOG_TAG, "--------------------------------------------------------");
		}
	}
	private void enableButton(boolean enable) {
		if (!enable) {
			cancelButton.setEnabled(false);
			runButton.setEnabled(false);
			usbButton.setEnabled(false);
		} else {
			cancelButton.setEnabled(true);
			runButton.setEnabled(true);
			usbButton.setEnabled(true);
		}
	}
	public String getUrlText() {
		return urlEditText != null && urlEditText.getText() != null
				? urlEditText.getText().toString()
				: DEFAULT_URL;
	}
	private RequestType getCurrentRequestType(){
		String url = getUrlText();
		if (url.contains("screenshot.jpg")) {
			return RequestType.SCREENSHOT;
		}
		if (url.contains("h264")) {
			return RequestType.MIRROR;
		}
		return null;
	}
	public void setupWindow() {
		//we use this api to popup Float window.
		if (FloatWindow.get() != null) {
			FloatWindow.destroy();
		}

		View baseView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.container_layout,null);
		surfaceView = (SurfaceView) baseView.findViewById(R.id.surface_view);
		mSurfaceHolder = surfaceView.getHolder();
		surfaceView.setOnTouchListener(monTouchListener);
		mPaired = false;
		mInputChannel = null;
		mSurfaceHolder.addCallback(this);
		//mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		FloatWindow
				.with(getApplicationContext())
				.setView(baseView)
				.setWidth(configuration.ScreenWIDTH, 1.0f)
				.setHeight(configuration.ScreenHEIGHT, 1.0f)
				.setX(0)
				.setY(0)
				.setMoveType(MoveType.slide)
				.setFilter(true)
				.setMoveStyle(500, new BounceInterpolator())
				.build();
		FloatWindow.get().show();

		if (getCurrentRequestType() == RequestType.MIRROR) {
			Log.d(LOG_TAG,"mirror enable");
			enableLongStream(true);
			//CodecPlayer Start.
			//mRenderManager = RenderManager.getInstance(mSurfaceHolder.getSurface());
			//connectPeer(mRenderManager);
		} else {

			enableLongStream(false);
		}
	}
	public void enableLongStream(boolean onOff){
		isLongStream = onOff;
	}
	private File getDownloadTarget() {
		File downloadTarget = null;
		try {
			if (getCurrentRequestType() == RequestType.SCREENSHOT) {
				downloadTarget = File.createTempFile("screen_", "_shot", getCacheDir());
			} else {
				downloadTarget = File.createTempFile("screen_", "_mirror", getCacheDir());
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "Couldn't create cache file to download to");
		}
		return downloadTarget;
	}
	public void onRunButtonPressed_SHOT() {
		//run http request
		AsyncHttpGet asyncHttpGet = new AsyncHttpGet(getUrlText());
		File filename = getDownloadTarget();
		Log.d(LOG_TAG, "GET file name is " + filename.getAbsolutePath());
		mFutureShot = AsyncHttpClient.getDefaultInstance().executeFile(asyncHttpGet, filename.getAbsolutePath(), new AsyncHttpClient.FileCallback() {
			@Override
			public void onProgress(AsyncHttpResponse response, long downloaded, long total) {
				Log.d(LOG_TAG, "shot onProgress " + "total : " + total + ", downloaded : " + downloaded);
			}
			@Override
			public void onConnect(AsyncHttpResponse response) {
				Log.d(LOG_TAG, "shot request onConnect");
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						setupWindow();
					}
				});


			}
			@Override
			public void onCompleted(Exception e, AsyncHttpResponse response, File result) {
				if (e != null) {
					e.printStackTrace();
					return;
				}
				Log.d(LOG_TAG, "shot request onCompleted");
				if (surfaceView == null) return;
				surfaceViewWidth = surfaceView.getWidth();
				surfaceViewHeight = surfaceView.getHeight();
				if (surfaceViewWidth <= 0 || surfaceViewHeight <= 0) return ;

				Canvas canvas = mSurfaceHolder.lockCanvas(null);
				Bitmap bitmap = BitmapFactory.decodeFile(filename.getAbsolutePath());
				if (bitmap == null) return ;
				Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, surfaceViewWidth,surfaceViewHeight, true);
				if(scaledBitmap == null) Log.d(LOG_TAG, "cant decode cache buffer");
				canvas.drawBitmap(scaledBitmap, 10, 10, new Paint());
				bitmap.recycle();
				scaledBitmap.recycle();
				mSurfaceHolder.unlockCanvasAndPost(canvas);
				result.delete();
			}
		});
	}
	private static String getDefaultUserAgent() {
		String agent = System.getProperty("http.agent");
		return agent != null ? agent : ("Java" + System.getProperty("java.version"));
	}
	private AsyncHttpRequest prepareMirrorRequest(Uri uri) {
		Headers ret = new Headers();
		if (uri != null) {
			String host = uri.getHost();
			if (uri.getPort() != -1)
				host = host + ":" + uri.getPort();
			if (host != null)
				ret.set("Host", host);
		}
		ret.set("User-Agent", getDefaultUserAgent());
		ret.set("Connection", "close");
		ret.set("Range", "bytes=0-");
		ret.set("Icy-MetaData", "1");
		ret.set("Accept", "*/*");
		AsyncHttpRequest req= new AsyncHttpRequest(uri, "GET", ret);
		return req;
	}
	public void onRunButtonPressed_MIRROR() {
		//run http request
		AsyncHttpRequest asyncHttpGet = prepareMirrorRequest(Uri.parse(getUrlText()));
		mFutureMirror = AsyncHttpClient.getDefaultInstance().executeBuffer(asyncHttpGet, new AsyncHttpClient.BufferCallback() {
			@Override
			public void onCompleted(Exception e, AsyncHttpResponse source, Integer result) {
				if (e != null) {
					e.printStackTrace();
					return;
				}
				Log.d(LOG_TAG, "mirror request onCompleted");
			}
			@Override
			public void onData(AsyncHttpResponse response, long downloaded, ByteBufferList bb) {
				//stream incoming from remote end
				Log.d(LOG_TAG, "mirror onData : " + downloaded);
				while (mPaired == false) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				//Log.d("System.err", Log.getStackTraceString(new Throwable()));
				//save data in local file for debug
				try {
					if (mStreamRecord && fileMirror == null) {
						File filenameMirror= getDownloadTarget();
						fileMirror = new FileOutputStream(filenameMirror);
						Log.d(LOG_TAG, "get mirror file " + filenameMirror.getAbsolutePath());
					}

					if(fileMirror != null) {
						fileMirror.write(bb.getAllByteArray());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				mStreamBuffer.enqueue(new ByteArrayInputStream(bb.getAllByteArray()));
			}
			@Override
			public void onProgress(AsyncHttpResponse response, long downloaded, long total) {
				Log.d(LOG_TAG, "mirror onProgress " + "total : " + total + ", downloaded : " + downloaded);
			}
			@Override
			public void onConnect(AsyncHttpResponse response) {
				Log.d(LOG_TAG, "mirror request onConnect");
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						setupWindow();
						setupInput();
					}
				});
			}
		});
	}

	private void setupInput() {
		AsyncHttpClient.getDefaultInstance().websocket("ws://" + remoteIPV4Addr +":52174/input", "mirror",
				new AsyncHttpClient.WebSocketConnectCallback() {
					public void onCompleted(Exception ex, WebSocket webSocket) {
						if(ex != null) {
							ex.printStackTrace();
							return;
						}
						Log.d(LOG_TAG, "input channel created " + webSocket.toString());
						mInputChannel = webSocket;
						mInputChannel.setStringCallback(new WebSocket.StringCallback() {
							@Override
							public void onStringAvailable(String s) {
								try {
									JSONObject jsonObject = new JSONObject(s);
									String type;
									Log.d(LOG_TAG, "incoming event : " + s);
									type = jsonObject.getString("type");

									if ("displaySize".equals(type)) {

										int width = jsonObject.optInt("screenWidth");
										int height = jsonObject. optInt("screenHeight");
										boolean hasNav = jsonObject.optBoolean("nav");
										int rotation = jsonObject.optInt("rotation");
										Log.d(LOG_TAG , "change oritation : width = " + width +
																				", height = " + height +
																				", has Nav = " + hasNav +
																				", rotation = " + rotation);
										//resize surfaceView

									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						});
						try {
							JSONObject msg = new JSONObject();
							String type = new String("type");
							msg.put(type, "wakeup");
							mInputChannel.send(msg.toString());
							msg.put(type, "authenticated");
							mInputChannel.send(msg.toString());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
	}
	/*private void resizeSurfaceView()
	{
		int width = mediaPlayer.getVideoWidth();
		int height = mediaPlayer.getVideoHeight();
		Point surfaceViewSize = measureSurfaceViewSize(width, height);
		RelativeLayout.LayoutParams surfaceLayoutParams = (LayoutParams) surfaceView.getLayoutParams();

		surfaceLayoutParams.width = surfaceViewSize.x;
		surfaceLayoutParams.height = surfaceViewSize.y;

		Log.d("size", " new size = " + surfaceViewSize);
		surfaceLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//重新设置布局
		mSurfaceView.setLayoutParams(surfaceLayoutParams);
	}

	//根据视频宽高和父View的宽高计算SurfaceView的宽高
	private Point measureSurfaceViewSize(int width, int height) {
		float parentWh = getMeasuredWidth() * 1.0f / getMeasuredHeight();
		float videoWh = width * 1.0f / height;
		Point surfaceViewSize = new Point();
		if (parentWh >= videoWh) {
			surfaceViewSize.y = getMeasuredHeight();
			surfaceViewSize.x = (int) (surfaceViewSize.y * videoWh);
		} else {
			surfaceViewSize.x = getMeasuredWidth();
			surfaceViewSize.y = (int) (surfaceViewSize.x / videoWh);
		}

		return surfaceViewSize;
	}*/
	private RectF calcViewScreenLocation(View view) {
		int[] location = new int[2];
		view.getLocationOnScreen(location);
		return new RectF(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
	}
	View.OnTouchListener monTouchListener  = new View.OnTouchListener(){
		@Override
		public boolean onTouch(View v, MotionEvent event){
			Log.d(LOG_TAG,"touch event:");
			JSONObject msg = new JSONObject();
			try {
				String type;
				//build x,y
				float x = event.getRawX();
				float y = event.getRawY();
				Log.d(LOG_TAG,"touch event: [ " + x + " , " + y + "],[ " + mMirrorScreenRect.toString() + "]");
				if (mMirrorScreenRect.contains(x, y)) {
					MotionEvent cloneEvent = MotionEvent.obtain(event);
					cloneEvent.offsetLocation(-mMirrorScreenRect.left, -mMirrorScreenRect.top);
					Log.d(LOG_TAG, "clone event:" + cloneEvent.toString());
					x = cloneEvent.getRawX();
					y = cloneEvent.getRawY();
					x = x * configuration.REMOTE_WIDTH / configuration.ScreenWIDTH;
					y = y * configuration.REMOTE_HEIGHT / configuration.ScreenHEIGHT;

					Log.d(LOG_TAG,"touch event (calc): [ " + x + " , " + y + "]");
					type = new String("clientX");
					msg.put(type, x);
					type = new String("clientY");
					msg.put(type, y);

					type = new String("type");
					//1 build action type
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							msg.put(type, "mousedown");
							break;
						case MotionEvent.ACTION_MOVE:
							msg.put(type, "mousemove");
							break;
						case MotionEvent.ACTION_UP:
							msg.put(type, "mouseup");
							break;
					}
					if (mInputChannel != null) {
						Log.d(LOG_TAG, "send input event to remote end");
						mInputChannel.send(msg.toString());
					}

				} else {
					Log.d(LOG_TAG, "out of screen rect");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return true;
		}
	};

	public void onUsbButtonPressed() {
		//do usb relative op.
	}
	public void onCancelButtonPressed() {
		//cancel http request
		if (getCurrentRequestType() == RequestType.MIRROR) {
			mFutureMirror.cancel();
		} else {
			mFutureShot.cancel();
		}
	}

	protected final View.OnClickListener onClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.button_run:
					setupConfiguration();
					if (getCurrentRequestType() == RequestType.MIRROR) {
						onRunButtonPressed_MIRROR();
					} else {
						onRunButtonPressed_SHOT();
					}
					break;
				case R.id.button_cancel:
					onCancelButtonPressed();
					break;
				case R.id.button_usb:
					onUsbButtonPressed();
					break;
			}
		}
	};
	private LinearLayout responseLayout;
	private boolean useHttps = true;
	private boolean enableLogging = true;
	public String getDefaultHeaders() {
		return "Range=bytes=0-";
	}
	public String getDefaultURL() {
		return PROTOCOL + DEFAULT_URL;
	}
	public boolean isRequestHeadersAllowed() {
		return true;
	}
	public boolean isRequestBodyAllowed() {
		return false;
	}
	public boolean isConnectAllowed() {
		return true;
	}
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		processExtraData();
	}
	private void processExtraData(){
		Intent intent = getIntent();
		remoteIPV4Addr = intent.getStringExtra("addr");
		Log.d(LOG_TAG, "input IPV4Addr " + remoteIPV4Addr);
		DEFAULT_URL = new String(remoteIPV4Addr + ":52174/h264");
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//if (!PermissionActivity.checkAndRequestPermission(this, REQUIRE_PERMISSIONS)) {
		processExtraData();
		setContentView(R.layout.parent_layout);
		setTitle("Network Activity");

		setHomeAsUpEnabled();

		urlEditText = (EditText) findViewById(R.id.edit_url);
		headersEditText = (EditText) findViewById(R.id.edit_headers);
		bodyEditText = (EditText) findViewById(R.id.edit_body);
		customFieldsLayout = (LinearLayout) findViewById(R.id.layout_custom);
		runButton = (Button) findViewById(R.id.button_run);
		usbButton = (Button) findViewById(R.id.button_usb);
		cancelButton = (Button) findViewById(R.id.button_cancel);
		LinearLayout headersLayout = (LinearLayout) findViewById(R.id.layout_headers);
		LinearLayout bodyLayout = (LinearLayout) findViewById(R.id.layout_body);
		responseLayout = (LinearLayout) findViewById(R.id.layout_response);

		urlEditText.setText(getDefaultURL());
		headersEditText.setText(getDefaultHeaders());

		bodyLayout.setVisibility(isRequestBodyAllowed() ? View.VISIBLE : View.GONE);
		headersLayout.setVisibility(isRequestHeadersAllowed() ? View.VISIBLE : View.GONE);

		runButton.setOnClickListener(onClickListener);
		usbButton.setOnClickListener(onClickListener);
		if (! isConnectAllowed()) {
			enableButton(false);
		}
		if (cancelButton != null) {
			cancelButton.setVisibility(View.VISIBLE);
			cancelButton.setOnClickListener(onClickListener);
		}
	}

	private void setupConfiguration() {
		AsyncHttpGet asyncHttpGet = new AsyncHttpGet("http://" + remoteIPV4Addr + ":52174/config");
		AsyncHttpClient.getDefaultInstance().executeJSONObject(asyncHttpGet, new AsyncHttpClient.JSONObjectCallback() {
			@Override
			public void onCompleted(Exception e, AsyncHttpResponse source, JSONObject jsonObj) {
				if (jsonObj == null) return;
				configuration.LISTEN_PORT = jsonObj.optInt("LISTEN_PORT");
				configuration.BITRATE = jsonObj.optInt("BITRATE");
				configuration.FRAME_RATE = jsonObj.optInt("FRAME_RATE");
				configuration.IFRAME_INTERVAL = jsonObj.optInt("IFRAME_INTERVAL");
				configuration.REMOTE_HEIGHT = jsonObj.optInt("ScreenHEIGHT");
				configuration.REMOTE_WIDTH = jsonObj.optInt("ScreenWIDTH");
				configuration.TIMEOUT_US = jsonObj.optInt("TIMEOUT_US");
				configuration.MIME_TYPE = jsonObj.optString("MIME_TYPE");
				configuration.dump();
			}
		});
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHomeAsUpEnabled() {
		if (Integer.valueOf(Build.VERSION.SDK) >= 11) {
			if (getActionBar() != null)
				getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}
	private void connectPeer(StreamUploadConnector peer) {
		mStreamBuffer.connect(peer);
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,    int height) {
		if (getCurrentRequestType() == RequestType.MIRROR) {
			//mView = findViewById(R.id.surfaceViewA);
			Log.d(LOG_TAG, "setup codec player");
			if (mPaired == false) {
				mRenderManager = RenderManager.getInstance(holder.getSurface());
				connectPeer(mRenderManager);
				mPaired = true;
			} else {
				//mRenderManager.updateSurface(holder.getSurface());
			}
		}
		mMirrorScreenRect = calcViewScreenLocation(surfaceView);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

}
