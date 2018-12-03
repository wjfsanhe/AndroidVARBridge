package com.koushikdutta.async.sample.network;

import android.util.Log;

import com.koushikdutta.async.sample.BuildConfig;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.NoSuchElementException;


/*create by wangjf on 2018/10/29*/

public class StreamBuffer extends StreamUploadConnector{
	private static final String TAG = StreamBuffer.class.getSimpleName();

	private int mTotalBytes = 0;


	// The follow flag constants MUST stay in sync with their equivalents
	// in MediaCodec.h !
	/**
	 * This indicates that the (encoded) buffer marked as such contains
	 * the data for a key frame.
	 *
	 * @deprecated Use {@link #BUFFER_FLAG_KEY_FRAME} instead.
	 */
	private static final int BUFFER_FLAG_SYNC_FRAME = 1;

	/**
	 * This indicates that the (encoded) buffer marked as such contains
	 * the data for a key frame.
	 */
	private static final int BUFFER_FLAG_KEY_FRAME = 1;

	/**
	 * This indicated that the buffer marked as such contains codec
	 * initialization / codec specific data instead of media data.
	 */
	private static final int BUFFER_FLAG_CODEC_CONFIG = 2;

	/**
	 * This signals the end of stream, i.e. no buffers will be available
	 * after this, unless of course, {@link #flush} follows.
	 */
	private static final int BUFFER_FLAG_END_OF_STREAM = 4;

	/*
	*	encoder relative type
	*
	*/
	private final int OP_STATUS_SUCCESS = 0;
	private final int OP_STATUS_CONTINUE = 1;
	private final int OP_STATUS_FAIL = -1;

	/*
	*  streaming structure
	*   ---------------------------------------------------------------------------------
	*  |DATA_SIZE|  PTS  | CONFIG | RAW_DATA |DATA_SIZE| PTS  | CONFIG | RAW_DATA |.....
	*   ---------------------------------------------------------------------------------
	*  |                           |		 |				           |          |
	*   \                         / \       / \                       / \        /
	*    \                       /   \     /   \                     /   \      /
	*     \_____________________/     \___/     \___________________/     \____/
	*                 |                 |                  |				 |
	*  1   ChunkItem.chunkInfo   ChunkItem.rawData         |                 |
	*      										2	ChunkItem.chunkInfo  ChunkItem.rawData
	*
	*												 3 .
	*
	*												 	 4 . ...........
	*/


	private enum PHASE {
		PREPARE,
		FRAME,
		IDLE
	}
	private final int DATA_TYPE_HEADER = 0;
	private final int DATA_TYPE_PAYLOAD =1;
	private int mCurrentDataType = DATA_TYPE_HEADER;
	private boolean mDataContinue = false;
	private PHASE mPhase;
	private byte[] mHeader_SPS_PPS;
	private LinkedList<ChunkItem> mBufferPoolList = null;
	private static StreamBuffer mInstance = null;
	private ChunkItem mCurrentChunkItem;
	private Item mCurrentHeaderItem;

	public StreamBuffer(){
		init();
	}
	private StreamUploadConnector mPeer;

	//construct one connector peer.
	@Override
	public void connect(StreamUploadConnector peer) {
		if(peer == this || peer == null) return;
		mPeer = peer;
		mPeer.connect(this);
	}

	/*------------------------------------------------------------------
	*
	*  StreamBuffer 				RenderManager
	*
	*
	*    network data (config or frame data)
 	*     |
	*    \|/
	*    put	---------notify------------> onPut
	*    										|
	*     										|
	*     									   \|/
	*    onGet  <---------------------------- get
	*           ---------- return feedback----->
	*
	*-------------------------------------------------------------------
	*/


	@Override
	public void put(int idx){
		mPeer.onPut(idx);
	}

	@Override
	public ChunkItem onGet(int idx){
		ChunkItem ret = null;
		Log.d(TAG, "onGet " + idx);
		switch (idx) {
			case DATA_IDX_CONFIG:
				ret = new ChunkItem();
				ret.chunkInfo = new int[3];
				ret.chunkInfo[CHUNK_CONFIG_DATASIZE] = mHeader_SPS_PPS.length;
				ret.chunkInfo[CHUNK_CONFIG_PTS] = 0;
				ret.chunkInfo[CHUNK_CONFIG_FLAG] = BUFFER_FLAG_CODEC_CONFIG;
				ret.rawData = mHeader_SPS_PPS;
				break;
			case DATA_IDX_FRAME:
				int size;
				try {
					//synchronized (this)
					{
						/*try {
							if(size == 0) wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}*/
						synchronized (StreamBuffer.this) {
							size = mBufferPoolList.size();
							if (size != 0) {
								ret = mBufferPoolList.pop();
							} else {
								Log.d(TAG, "list is null");
								ret = null;
							}
						}
					}
					if (ret != null)
					mTotalBytes -= ret.chunkInfo[CHUNK_CONFIG_DATASIZE];
				} catch (NoSuchElementException e) {
					Logger.d("list is empty");
				}
				break;
		}
		return ret;
	}
	public static StreamBuffer getInstance(){
		if (mInstance == null){
			mInstance = new StreamBuffer();
		}
		initLogger(TAG);
		return mInstance;
	}
	private static void initLogger(String tag) {
		FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
				.tag(tag) // 全局tag
				.showThreadInfo(false)
				.methodCount(0)
				.build();

		Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
			@Override
			public boolean isLoggable(int priority, String tag) {
				return BuildConfig.DEBUG;
			}
		});
	}
	private int[] byteArray2IntArray(byte buf[], ByteOrder order) {
		int intArr[] = new int[buf.length / 4];
		int offset = 0;
		for (int i = 0; i < intArr.length; i++) {
			if (order == ByteOrder.BIG_ENDIAN) {
				intArr[i] = (buf[3 + offset] & 0xFF) | ((buf[2 + offset] & 0xFF) << 8) |
						((buf[1 + offset] & 0xFF) << 16) | ((buf[0 + offset] & 0xFF) << 24);
			} else {
				intArr[i] = (buf[0 + offset] & 0xFF) | ((buf[1 + offset] & 0xFF) << 8) |
						((buf[2 + offset] & 0xFF) << 16) | ((buf[3 + offset] & 0xFF) << 24);
			}
			offset += 4;
		}
		return intArr;
	}
	public void reset() {
		mPhase = PHASE.PREPARE;
		mTotalBytes = 0;
		mBufferPoolList = new LinkedList<ChunkItem>();
		mDataContinue = false;
		mCurrentHeaderItem = new Item();
		mCurrentDataType = DATA_TYPE_HEADER;
	}

	private void init() {
		mFetcher[DATA_TYPE_HEADER] = new HeaderFetcher();
		mFetcher[DATA_TYPE_PAYLOAD] = new PayloadFetcher();
		reset();
	}

	private abstract class Fetcher {
		public abstract int processInput(InputStream in);
		protected int fetchRequestBytes(InputStream in, Item item){
			byte[] load = item.rawData;
			int offset = item.offset;
			int len = item.remaining;
			do {
				int l = 0;
				try {
					l = in.read(load, offset, len);
				} catch (IOException e) {
					e.printStackTrace();
					return -1;
				}
				if (l > 0) {
					len -= l;
					offset += l;
					if (item != null) {
						item.offset = offset;
						item.remaining = len;
					}
				}
				if (item != null && item.remaining == 0) {
					//all data has been received.
					break;
				}
				Log.d(TAG,"read back " + l + " bytes.");
				if (l < 0) {
/*					try {
						//Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}*/
					return -1;
				}
				if (Thread.currentThread().isInterrupted()) return -1; 
			} while (len > 0);
			Log.d(TAG, "offset = " + offset + " bytes, return to fetcher");
			return 0;
		}
	}
	private class HeaderFetcher extends Fetcher {
		//put header into StreamBuffer,and fill ChunkItem
		public int processInput(InputStream in) {
			//read header.
			if (mDataContinue == false) {
				byte[] payload = new byte[12];
				mCurrentHeaderItem.rawData = payload;
				mCurrentHeaderItem.offset = 0;
				mCurrentHeaderItem.remaining = 12;
			}

			if (fetchRequestBytes(in, mCurrentHeaderItem) < 0) {
				mDataContinue = true;
				return OP_STATUS_FAIL;
			}
			mDataContinue = false;
			mCurrentChunkItem = new ChunkItem();
			mCurrentChunkItem.chunkInfo = byteArray2IntArray(mCurrentHeaderItem.rawData, ByteOrder.LITTLE_ENDIAN);
			
			int ret = OP_STATUS_SUCCESS;
			if ((mCurrentChunkItem.chunkInfo[CHUNK_CONFIG_FLAG] & ~0x7) != 0) {
				Log.d(TAG,"invalid Frame Type");
				ret = OP_STATUS_FAIL;
			}
			mCurrentDataType = DATA_TYPE_PAYLOAD;
			Log.d(TAG,String.format("Header: 0x%08x 0x%08x 0x%08x\n", mCurrentChunkItem.chunkInfo[0],
					mCurrentChunkItem.chunkInfo[1], mCurrentChunkItem.chunkInfo[2]));
			return ret;
 		}
	}
	private class PayloadFetcher extends Fetcher {
		public int processInput(InputStream in) {
			int payloadLen = mCurrentChunkItem.chunkInfo[CHUNK_CONFIG_DATASIZE] - 8;

			if (mDataContinue == false) {
				byte[] payload = new byte[payloadLen];
				mCurrentChunkItem.rawData = payload;
				mCurrentChunkItem.offset = 0;
				mCurrentChunkItem.remaining = payloadLen;
				Log.d(TAG, "payLoadlen = " + payloadLen);
			}
			if (fetchRequestBytes(in, mCurrentChunkItem ) < 0)
			{
				//mPhase = PHASE.FRAME;
				mDataContinue = true;
				return OP_STATUS_FAIL;
			}

			mDataContinue = false;
			mCurrentDataType = DATA_TYPE_HEADER;
			if (mPhase == PHASE.PREPARE) {
				mHeader_SPS_PPS = mCurrentChunkItem.rawData;
				Log.d(TAG,"SPS_PPS =>");
				StringBuilder builder = new StringBuilder();
				for (int i=0 ; i< mHeader_SPS_PPS.length; i++ ) {
					builder.append(String.format(" 0x%02x ",mHeader_SPS_PPS[i]));
				}
				Log.d(TAG,builder.toString());
				mPhase = PHASE.FRAME;
				put(DATA_IDX_CONFIG); //notify
				return OP_STATUS_CONTINUE;
			}

			mCurrentChunkItem.chunkInfo[CHUNK_CONFIG_DATASIZE] = payloadLen;//modify payload len to real size.
			synchronized (StreamBuffer.this) {
				mBufferPoolList.add(mCurrentChunkItem);
			}
			put(DATA_IDX_FRAME); //notify
			//reset frame flag.
			mTotalBytes += mCurrentChunkItem.chunkInfo[CHUNK_CONFIG_DATASIZE];
			Log.d(TAG, "Total = " + mTotalBytes + ", item in list is " + mBufferPoolList.size());
			return OP_STATUS_SUCCESS;
		}
	}

	private Fetcher[] mFetcher = new Fetcher[2];
	
	public int enqueue(InputStream in) {
		int ret = OP_STATUS_FAIL;
		do {
			/*if (mDataContinue == true) {
				ret = mFetcher[DATA_TYPE_PAYLOAD].processInput(in);
			} else {
				ret = mFetcher[DATA_TYPE_HEADER].processInput(in);
				if (ret == OP_STATUS_SUCCESS) {
					//if the HEADER fetched successfully,then switch to the second step.
					ret = mFetcher[DATA_TYPE_PAYLOAD].processInput(in);
				}
			}*/
			ret = mFetcher[mCurrentDataType].processInput(in);			

		} while (ret != OP_STATUS_FAIL);
		Log.d(TAG, "exit while loop");
		return ret;
	}
	public void dump() {
		StringBuilder build = new StringBuilder("");
		build.append("--------------- StreamBuffer-----------------");
		build.append(String.format(" pool mTotalBytes = %d\n", mTotalBytes));

		String currentPhase;
		switch (mPhase) {
			case PREPARE:
				currentPhase = new String("CurrentPhase : PREPARE \n");
				break;
			case FRAME:
				currentPhase = new String("CurrentPhase : FRAME \n");
				break;
			default:
				currentPhase = new String("CurrentPhase : UNKNOWN \n");
				break;
		}
		build.append(currentPhase);

		Log.d(TAG,build.toString());
	}
}
