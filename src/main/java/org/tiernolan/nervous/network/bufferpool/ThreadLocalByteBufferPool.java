package org.tiernolan.nervous.network.bufferpool;


public class ThreadLocalByteBufferPool extends ThreadLocal<ByteBufferQueue> {
	
	private final int size;
	
	public ThreadLocalByteBufferPool(int size) {
		this.size = size;
	}
	
	@Override
	public ByteBufferQueue initialValue() {
		return new ByteBufferQueue(size);
	}

}
