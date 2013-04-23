package org.tiernolan.nervous.network.bufferpool;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class ByteBufferQueue {

	private final int size;
	private final ArrayBlockingQueue<FlexibleReference<ByteBuffer>> queue = new ArrayBlockingQueue<FlexibleReference<ByteBuffer>>(4);

	public ByteBufferQueue(int size) {
		this.size = size;
	}

	public Reference<ByteBuffer> get() {
		FlexibleReference<ByteBuffer> ref;
		while ((ref = queue.poll()) != null && ref.setHard())
			;

		if (ref != null) {
			return ref;
		} else {
			return new FlexibleReference<ByteBuffer>(ByteBuffer.allocateDirect(size), true);
		}
	}
	
	public void put(Reference<ByteBuffer> ref) {
		if (ref instanceof FlexibleReference) {
			FlexibleReference<ByteBuffer> flexible = (FlexibleReference<ByteBuffer>) ref;
			flexible.setSoft();
			queue.offer(flexible);
		}
	}
}