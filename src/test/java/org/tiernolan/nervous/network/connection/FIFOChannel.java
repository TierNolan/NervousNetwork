package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.LinkedList;

public class FIFOChannel implements ByteChannel {
	
	private final LinkedList<Byte> fifo = new LinkedList<Byte>();

	public int read(ByteBuffer dst) throws IOException {
		int count = 0;
		while (dst.hasRemaining() && !fifo.isEmpty()) {
			dst.put(fifo.remove());
			count++;
		}
		return count;
	}

	public boolean isOpen() {
		return true;
	}

	public void close() throws IOException {
	}

	public int write(ByteBuffer src) throws IOException {
		int count = 0;
		while (src.hasRemaining()) {
			fifo.add(src.get());
			count++;
		}
		return count;
	}
	
	public void write(Integer ... bytes) {
		for (Integer i : bytes) {
			fifo.add((byte)(int) i);
		}
	}
	
	public void write(Long ... bytes) {
		for (Long l : bytes) {
			fifo.add((byte)(long) l);
		}
	}
	
	public Integer read() {
		Byte b = fifo.remove();
		if (b == null) {
			return null;
		}
		return (int)(byte) b;
	}

}
