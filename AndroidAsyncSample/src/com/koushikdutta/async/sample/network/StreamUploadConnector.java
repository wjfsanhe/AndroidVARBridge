package com.koushikdutta.async.sample.network;

public abstract  class StreamUploadConnector {
	public class ChunkItem extends  Item{
		public int[] chunkInfo; // 0 dataSize, 1 pts, 2 flag
	};
	public class Item {
		public byte[] rawData;
		public int offset;
		public int remaining;
	};
	public static final int DATA_IDX_CONFIG = 0;
	public static final int DATA_IDX_FRAME = 1;

	public static final int CHUNK_CONFIG_DATASIZE = 0;
	public static final int CHUNK_CONFIG_PTS = 1;
	public static final int CHUNK_CONFIG_FLAG = 2;
	public void connect(StreamUploadConnector peer){};
	public void put(int idx){};
	public void onPut(int idx){};
	public ChunkItem get(int idx){return null;};
	public ChunkItem onGet(int idx){return null;};
}
