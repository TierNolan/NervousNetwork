package org.tiernolan.nervous.network.bufferpool;


public class ThreadLocalByteBufferPool extends ThreadLocal<ByteBufferQueue> {
	
	private final int size;
	private final int depth;
	
	public ThreadLocalByteBufferPool(int size) {
		this(size, ByteBufferQueue.DEFAULT_DEPTH);
	}
	
	public ThreadLocalByteBufferPool(int size, int depth) {
		this.size = size;
		this.depth = depth;
	}
	
	@Override
	public ByteBufferQueue initialValue() {
		return new ByteBufferQueue(size, depth);
	}

}
