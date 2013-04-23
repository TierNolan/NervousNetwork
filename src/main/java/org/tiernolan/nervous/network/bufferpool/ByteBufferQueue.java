package org.tiernolan.nervous.network.bufferpool;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

public class ByteBufferQueue {
	
	protected final static int DEFAULT_DEPTH = 4;

	private final int size;
	private final ArrayQueue queue;

	public ByteBufferQueue(int size) {
		this(size, DEFAULT_DEPTH);
	}
	
	public ByteBufferQueue(int size, int depth) {
		this.queue = new ArrayQueue(depth);
		this.size = size;
	}

	public Reference<ByteBuffer> get() {
		FlexibleReference<ByteBuffer> ref;
		while ((ref = queue.poll()) != null && !ref.setHard())
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
	
	private class ArrayQueue {
		
		private final FlexibleReference<ByteBuffer>[] queue;
		private int read = 0;
		private int write = 0;
		private final int mask;
		
		@SuppressWarnings("unchecked")
		public ArrayQueue(int depth) {
			if (depth <= 0) {
				depth = 1;
			}
			depth--;
			for (int i = 1; i < 32; i *= 2) {
				depth |= depth >> i;
			}
			this.mask = depth;
			depth++;
			this.queue = new FlexibleReference[depth];
		}
		
		public FlexibleReference<ByteBuffer> poll() {
			if (read >= write) {
				return null;
			}
			return queue[mask & (read++)];
		}
		
		public boolean offer(FlexibleReference<ByteBuffer> ref) {
			if (write > read + mask) {
				return false;
			}
			queue[(write++) & mask] = ref;
			return true;
		}
		
	}
}