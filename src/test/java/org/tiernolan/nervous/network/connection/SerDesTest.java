package org.tiernolan.nervous.network.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import org.junit.Test;
import org.tiernolan.nervous.network.NetworkManagerImpl;
import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.connection.TestProtocol.GenericPacket;
import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SerDesTest  {
	
	@Test
	public void decodeTest() throws IOException {
		
		Protocol protocol = new TestProtocol();
		
		NetworkManager manager = new NetworkManagerImpl(protocol);
		
		TestNetwork network = new TestNetwork();
		
		StripedQueue<Packet> queue = new SimpleStripedQueue();
		
		Serdes serdes = new SerDesImpl(manager, network, queue);
		
		FIFOChannel channel = new FIFOChannel();
		
		writeIntPacket(channel, 7);
	
		serdes.read(channel);
		
		Packet p = queue.poll().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 7);		
	}
	
	@Test
	public void seekTest() throws IOException {
		
		Protocol protocol = new TestProtocol();
		
		NetworkManager manager = new NetworkManagerImpl(protocol);
		
		TestNetwork network = new TestNetwork();
		
		StripedQueue<Packet> queue = new SimpleStripedQueue();
		
		Serdes serdes = new SerDesImpl(manager, network, queue);
		
		FIFOChannel channel = new FIFOChannel();
		
		writeIntPacket(channel, 7);

		Random r = new Random();
		for (int i = 0; i < 100; i++) {
			byte b = (byte) r.nextInt();
			if (b == (byte) 0x55) {
				b++;
			}
			channel.write((int) b);
		}

		writeIntPacket(channel, 1234567);

		serdes.read(channel);
		
		Packet p = queue.poll().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 7);		
		
		p = queue.poll().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 1234567);
		
		Completable<Packet> completable = queue.poll();
		
		assertTrue("Unexpected packet decoded", completable == null);

	}
	
	@Test
	public void randomDecodeTest() throws IOException {
		Protocol protocol = new TestProtocol();

		NetworkManager manager = new NetworkManagerImpl(protocol);

		TestNetwork network = new TestNetwork();

		StripedQueue<Packet> queue = new SimpleStripedQueue();

		Serdes serdes = new SerDesImpl(manager, network, queue);

		FIFOChannel channel = new FIFOChannel();

		Random r = new Random();
		
		int[] values = new int[1000];
		
		for (int i = 0; i < values.length; i++) {
			int v = r.nextInt();
			writeIntPacket(channel, v);
			values[i] = v;
		}
		
		serdes.read(channel);
		
		for (int i = 0; i < values.length; i++) {
			Packet p = (Packet) queue.poll().getStriped();
			assertEquals("Packet decode failure", ((GenericPacket) p).getData(), values[i]);
		}
		
		assertTrue("Unexpected packet decoded", queue.poll() == null);
	}
	
	@Test
	public void encodeTest() throws IOException {
		
		TestProtocol protocol = new TestProtocol();

		NetworkManager manager = new NetworkManagerImpl(protocol);

		TestNetwork network = new TestNetwork();

		StripedQueue<Packet> queue = new SimpleStripedQueue();

		Serdes serdes = new SerDesImpl(manager, network, queue);
		
		FIFOChannel channel = new FIFOChannel();

		GenericPacket p = getPacket(7, protocol);
		
		serdes.writePacket(p);
		
		assertTrue("Network not in write request mode", network.getWriteRequest());
		
		serdes.write(channel);
		
		assertTrue("Network in write request mode", !network.getWriteRequest());
		
		checkIntPacket(channel, 7);
		
		p = getPacket(99L, protocol);
		
		serdes.writePacket(p);
		
		assertTrue("Network not in write request mode", network.getWriteRequest());
		
		serdes.write(channel);
		
		assertTrue("Network in write request mode", !network.getWriteRequest());

		checkLongPacket(channel, 99L);
		
	}
	
	@Test
	public void passthroughTest() throws IOException {
		TestProtocol protocol = new TestProtocol();

		NetworkManager manager = new NetworkManagerImpl(protocol);

		TestNetwork network = new TestNetwork();

		StripedQueue<Packet> queue = new SimpleStripedQueue();

		Serdes serdesEncoder = new SerDesImpl(manager, network, null);
		
		Serdes serdesDecoder = new SerDesImpl(manager, null, queue);

		FIFOChannel channel = new FIFOChannel();
		
		LinkedList<Long> longList = new LinkedList<Long>();

		LinkedList<Integer> intList = new LinkedList<Integer>();
		
		Random r = new Random();
		
		for (int i = 0; i < 1000; i++) {
			if (r.nextBoolean()) {
				Long l = r.nextLong();
				longList.add(l);
				serdesEncoder.writePacket(getPacket(l, protocol));
			} else {
				Integer j = r.nextInt();
				intList.add(j);
				serdesEncoder.writePacket(getPacket(j, protocol));
			}
			assertTrue("Network not in write request mode", network.getWriteRequest());
			
			serdesEncoder.write(channel);
			
			assertTrue("Network in write request mode", !network.getWriteRequest());
			
			serdesDecoder.read(channel);
		}
		
		while (!queue.isEmpty()) {
			GenericPacket p = (GenericPacket) queue.poll().getStriped();
			if (p.getData() instanceof Long) {
				assertEquals("Packet type and data mismatch", p.getType(), 1);
				assertEquals("Packet contains wrong data", p.getData(), longList.poll());
			} else {
				assertEquals("Packet type and data mismatch", p.getType(), 0);
				assertEquals("Packet contains wrong data", p.getData(), intList.poll());
			}
		}
	}

	private void writeIntPacket(FIFOChannel channel, int i) {
		channel.write(0xAA, 0x55, 0x00, 0x00, i >> 24, i >> 16, i >> 8, i);
	}
	
	private void writeLongPacket(FIFOChannel channel, long l) {
		channel.write(0xAAL, 0x55L, 0x00L, 0x01L, l >> 56, l >> 48, l >> 40, l >> 32, l >> 24, l >> 16, l >> 8, l);
	}
	
	public void checkIntPacket(FIFOChannel channel, int i) {
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0xAA);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x55);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x00);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x00);
		
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 24) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 16) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 8) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 0) & 0xFF));
	}
	
	public void checkLongPacket(FIFOChannel channel, long l) {
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0xAA);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x55);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x00);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x01);
		
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 56) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 48) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 40) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 32) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 24) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 16) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 8) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((l >> 0) & 0xFF));
	}
	
	public GenericPacket getPacket(final Object data, TestProtocol protocol) {
		int type = (data instanceof Long) ? 1 : 0;
		return protocol.new GenericPacket(type) {
			@Override
			public Object getData() {
				return data;
			}
		};
	}
	
	private class SimpleStripedQueue implements StripedQueue<Packet> {
		
		private LinkedList<Packet> queue = new LinkedList<Packet>();

		public boolean offer(Packet p) {
			return queue.offer(p);
		}

		public Packet peek() {
			return queue.peek();
		}

		public Completable<Packet> poll() {
			Packet p = queue.poll();
			if (p == null) {
				return null;
			}
			return new SimpleCompletableStriped(p);
		}

		public Completable<Packet> take() throws InterruptedException {
			Packet p = queue.poll();
			if (p == null) {
				throw new IllegalStateException("Attempt made to take packet when queue was empty");
			}
			return new SimpleCompletableStriped(p);		}

		public boolean isEmpty() {
			return queue.isEmpty();
		}
		
	}
	
	private class SimpleCompletableStriped implements Completable<Packet> {
		
		private final Packet packet;
		
		public SimpleCompletableStriped(Packet packet) {
			this.packet = packet;
		}

		public int getStripeId() {
			return packet.getStripeId();
		}

		public void done() {
		}

		public Packet getStriped() {
			return packet;
		}
		
	}

}
