package com.koushikdutta.async.sample.render;

//use this class to record Render parameter.

import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.koushikdutta.async.sample.network.StreamUploadConnector;

import java.nio.ByteBuffer;

public class RenderManager extends StreamUploadConnector {
	public class Config {
		public static final int LISTEN_PORT = 52174;
		public static final int ScreenWIDTH = 720;
		public static final int ScreenHEIGHT = 1280;
		public static final int FRAME_RATE = 30;
		public static final int IFRAME_INTERVAL = 1;
		public static final int TIMEOUT_US = 10 * 1000;
		public static final int BITRATE = 1000 * 1000;
		public static final String MIME_TYPE = "video/avc";//h264
	}
	private static final String TAG = RenderManager.class.getSimpleName();
	private static LooperThread mThread = null;
	private static Handler mThreadHandler = null;
	private static class LooperThread extends Thread {
		public Handler mHandler;
		@Override
		public void run() {
			super.run();
			Looper.prepare();
			mHandler=new Handler() {
				public void handleMessage(Message msg) {
					Log.i(TAG, String.valueOf(msg.what));
					switch (msg.what) {
						case DATA_IDX_CONFIG: //config
							//byte[] sps = {0, 0, 0, 1, 103, 100, 0, 40, -84, 52, -59, 1, -32, 17, 31, 120, 11, 80, 16, 16, 31, 0, 0, 3, 3, -23, 0, 0, -22, 96, -108};
							//byte[] pps = {0, 0, 0, 1, 104, -18, 60, -128};
							byte[] sps = new byte[17];
							byte[] pps = new byte[8];
							byte[] config = mInstance.get(DATA_IDX_CONFIG).rawData;
							System.arraycopy(config, 0, sps, 0, 17);
							System.arraycopy(config, 17, pps, 0, 8);
							Log.d(TAG, "pps is ==>");
							StringBuilder builder = new StringBuilder();
							for (int i = 0; i < pps.length; i++) {
								builder.append(String.format(" 0x%02x ", pps[i]));
							}
							Log.d(TAG, builder.toString());

							//set codec config .
							mPlayer.config(sps, pps);
							break;
						case DATA_IDX_FRAME:
							//frame data coming.
							if (mInstance.decode() == 0) {
								mInstance.onPut(DATA_IDX_FRAME);
							}
							break;
					}
				}
			};
			Looper.loop();
		}

		public Handler getHandler() {
			if(!this.isAlive()) {
				Log.d(TAG, "thread has not bee started");
				return null;
			}
			return mHandler;
		}
	}
	private static CodecPlayer mPlayer;
	private StreamUploadConnector mPeer;
	private static Surface mSurface;
	private static  RenderManager mInstance;
	@Override
	public void connect(StreamUploadConnector peer) {
		if(peer == this) return;
		mPeer = peer;
	}
	public static RenderManager getInstance(Surface surface){
		if (mInstance == null){
			mInstance = new RenderManager();
			mThread = new LooperThread();
		}
		init(surface);
		return mInstance;
	}

	private static void init(Surface surface){
		mSurface = surface;
		mPlayer = new CodecPlayer(mSurface);
		mThread.start();
		mThreadHandler = null;
	}

	private void exit() {

	}
	//call from another thread
	@Override
	public void onPut(int idx){
		Log.d(TAG,"onPut once ....");
		if (mThreadHandler == null) {
			mThreadHandler = mThread.getHandler();
		}
		if (mThreadHandler == null) return;

		Message m=mThreadHandler.obtainMessage();
		m.what = idx;
		mThreadHandler.sendMessage(m);
	}
	//call from this thread.
	@Override
	public ChunkItem get(int idx){
		ChunkItem item = mPeer.onGet(idx);
		if (item != null) {
			Log.d(TAG, "onGet ret size " + item.chunkInfo[CHUNK_CONFIG_DATASIZE]);
		}
		return item;
	}

	public int decode() {
		MediaCodec decoder = mPlayer.getDecoder();
		while (decoder == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			decoder = mPlayer.getDecoder();
		}
		StreamUploadConnector.ChunkItem item = get(DATA_IDX_FRAME);
		if (item != null){
			int inIndex = decoder.dequeueInputBuffer(1000);
			if (inIndex >= 0) {
				ByteBuffer buffer = decoder.getInputBuffer(inIndex);
				buffer.put(item.rawData);
				if (item.chunkInfo[CHUNK_CONFIG_DATASIZE] < 0) {
					Log.d(TAG, "Input buffer eos");
					decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
				} else {
					Log.d(TAG, "-----decode one frame");
					decoder.queueInputBuffer(inIndex, 0, item.chunkInfo[CHUNK_CONFIG_DATASIZE],
							item.chunkInfo[CHUNK_CONFIG_PTS], item.chunkInfo[CHUNK_CONFIG_FLAG]);
					//0, item.chunkInfo[CHUNK_CONFIG_FLAG]);
				}
			}
		} else {
			return -1;
		}
		//SystemClock.sleep(120);
		int outIndex = 0;
		do {
			MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			outIndex = decoder.dequeueOutputBuffer(info, 0);
			if (outIndex >= 0) {
				boolean doRender = (info.size != 0);
				decoder.releaseOutputBuffer(outIndex, doRender);
			} else {
				Log.d(TAG, "-----try again later " + outIndex);
			}
			Log.d(TAG, "-------+++++ cycle " + outIndex);
		} while (outIndex >= 0);
		return 0;
	}
}
