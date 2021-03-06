package org.tiernolan.nervous.network.bufferpool;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

public class ByteBufferPool {
	
	public final ThreadLocalByteBufferPool pool32;
	public final ThreadLocalByteBufferPool[] pools;
	
	public ByteBufferPool(int maxSize) {
		this(maxSize, ByteBufferQueue.DEFAULT_DEPTH);
	}

	public ByteBufferPool(int maxSize, int depth) {
		if (maxSize < 0) {
			throw new IllegalArgumentException("Maximum size cannot be less than zero");
		} else if (maxSize < 32) {
			maxSize = 32;
		}
	    int numPools = 27 - Integer.numberOfLeadingZeros(maxSize - 1); 
	    
	    pool32 = new ThreadLocalByteBufferPool(32, depth);
	    
	    pools = new ThreadLocalByteBufferPool[numPools];
	    
	    int size = 64;
	    for (int i = 0; i < numPools; i++) {
	    	pools[i] = new ThreadLocalByteBufferPool(size, depth);
	    	size *= 2;
	    }
	}
	
	public Reference<ByteBuffer> get(int size) {
		ThreadLocalByteBufferPool pool = getThreadLocalByteBufferPool(size);
		ByteBufferQueue queue = pool.get();
		Reference<ByteBuffer> ref = queue.get();
		ByteBuffer buf = ref.get();
		buf.rewind();
		buf.limit(size);
		return ref;
	}
	
	public void put(Reference<ByteBuffer> ref) {
		ByteBuffer buf = ref.get();
		ThreadLocalByteBufferPool pool = getThreadLocalByteBufferPool(buf.capacity());
		ByteBufferQueue queue = pool.get();
		queue.put(ref);
	}
	
	private ThreadLocalByteBufferPool getThreadLocalByteBufferPool(int size) {
		if (size <= 32) {
			return pool32;
		} else {
			int poolIndex = 26 - Integer.numberOfLeadingZeros(size - 1);
			if (poolIndex >= pools.length) {
				throw new IllegalArgumentException("Buffer size of " + size + " exceeded maximum size of " + (32 << pools.length));
			}
			return pools[poolIndex];
		}
	}

}
