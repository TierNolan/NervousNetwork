package org.tiernolan.nervous.network.bufferpool;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ByteBufferPoolTest {
	
	private final int SIZE_TESTS = 100;
	private final int MAX_SIZE = 32 * 1024 * 1024;

	@Test
	public void testSize() {
		Random r = new Random();
		
		ByteBufferPool pool = new ByteBufferPool(MAX_SIZE);
		
		for (int i = 0; i < SIZE_TESTS; i++) {
			
			int size = (int) Math.pow(2, r.nextFloat() * 20);
			
			if (size <= MAX_SIZE) {
				testBuffer(pool, size);
			}
		}
		
		for (int i = 1; i <= MAX_SIZE; i *= 2) {
			testBuffer(pool, i - 1);
			testBuffer(pool, i);
			if (i < MAX_SIZE) {
				testBuffer(pool, i + 1);
			}
		}
	}
	
	
	private final void testBuffer(ByteBufferPool pool, int size) {
		
		Reference<ByteBuffer> ref = pool.get(size);
		
		int capacity = ref.get().capacity();
		assertTrue("Insufficent buffer capacity of " + capacity + " " + size + " required", capacity >= size);
		
		if (capacity > 32) {
			assertTrue("Buffer capacity of " + capacity + " uses to much RAM only " + size + " required", capacity < (size * 2));
		}
		
		pool.put(ref);
	}
	
}
