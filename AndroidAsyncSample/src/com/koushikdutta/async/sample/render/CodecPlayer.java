package com.koushikdutta.async.sample.render;
//use media codec to render cast screen.

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

//created by wangjf on  2018/11/01

public class CodecPlayer {
	private final String TAG = CodecPlayer.class.getSimpleName();
	private final Surface mSurface;
	private MediaCodec mDecoder;
	//this player must be run in one seperate thread.
	public CodecPlayer(Surface surface) {
		mSurface = surface;
	}

	public MediaCodec getDecoder() {
		return mDecoder;
	}

	public void config(MediaFormat mediaFormat) {
		try {
			mDecoder = MediaCodec.createDecoderByType(RenderManager.Config.MIME_TYPE);
			mDecoder.configure(mediaFormat, mSurface, null, 0);
			mDecoder.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void config(byte[] csd0, byte[] csd1) {
		MediaFormat format = new MediaFormat();
		//MediaFormat format = MediaFormat.createVideoFormat(RenderManager.Config.MIME_TYPE, width, height);
		format.setString(MediaFormat.KEY_MIME, RenderManager.Config.MIME_TYPE);
		format.setInteger(MediaFormat.KEY_WIDTH, RenderManager.Config.ScreenWIDTH);
		format.setInteger(MediaFormat.KEY_HEIGHT, RenderManager.Config.ScreenHEIGHT);
		format.setByteBuffer("csd-0", ByteBuffer.wrap(csd0));//sps
		format.setByteBuffer("csd-1", ByteBuffer.wrap(csd1));//pps
		format.setInteger(MediaFormat.KEY_FRAME_RATE, RenderManager.Config.BITRATE);
		config(format);
	}

}
