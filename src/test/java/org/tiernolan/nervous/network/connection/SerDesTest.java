package org.tiernolan.nervous.network.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import org.junit.Test;
import org.tiernolan.nervous.network.NetworkManagerImpl;
import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.connection.SimpleProtocol.GenericPacket;
import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SerDesTest  {
	
	@Test
	public void decodeTest() throws IOException {
		
		Protocol<SimpleConnection> protocol = new SimpleProtocol();
		
		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);
		
		SimpleNetwork network = new SimpleNetwork();
		
		StripedQueue<Packet<SimpleConnection>> queue = new SimpleStripedQueue();
		
		Serdes<SimpleConnection> serdes = new SerdesImpl<SimpleConnection>(manager, network, queue);
		
		SimpleFIFOChannel channel = new SimpleFIFOChannel();
		
		writeIntPacket(channel, 7);
	
		serdes.read(channel);
		
		Packet<SimpleConnection> p = queue.poll().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 7);		
		
	}
	
	@Test
	public void seekTest() throws IOException {
		
		Protocol<SimpleConnection> protocol = new SimpleProtocol();
		
		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);
		
		SimpleNetwork network = new SimpleNetwork();
		
		StripedQueue<Packet<SimpleConnection>> queue = new SimpleStripedQueue();
		
		Serdes<SimpleConnection> serdes = new SerdesImpl<SimpleConnection>(manager, network, queue);
		
		SimpleFIFOChannel channel = new SimpleFIFOChannel();
		
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
		
		Packet<SimpleConnection> p = queue.poll().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 7);		
		
		p = queue.poll().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 1234567);
		
		Completable<Packet<SimpleConnection>> completable = queue.poll();
		
		assertTrue("Unexpected packet decoded", completable == null);

	}
	
	@Test
	public void randomDecodeTest() throws IOException {
		Protocol<SimpleConnection> protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		SimpleNetwork network = new SimpleNetwork();

		StripedQueue<Packet<SimpleConnection>> queue = new SimpleStripedQueue();

		Serdes<SimpleConnection> serdes = new SerdesImpl<SimpleConnection>(manager, network, queue);

		SimpleFIFOChannel channel = new SimpleFIFOChannel();

		Random r = new Random();
		
		int[] values = new int[1000];
		
		for (int i = 0; i < values.length; i++) {
			int v = r.nextInt();
			writeIntPacket(channel, v);
			values[i] = v;
		}
		
		serdes.read(channel);
		
		for (int i = 0; i < values.length; i++) {
			Packet<SimpleConnection> p = (Packet<SimpleConnection>) queue.poll().getStriped();
			assertEquals("Packet decode failure", ((GenericPacket) p).getData(), values[i]);
		}
		
		assertTrue("Unexpected packet decoded", queue.poll() == null);
	}
	
	@Test
	public void encodeTest() throws IOException {
		
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		SimpleNetwork network = new SimpleNetwork();

		StripedQueue<Packet<SimpleConnection>> queue = new SimpleStripedQueue();

		Serdes<SimpleConnection> serdes = new SerdesImpl<SimpleConnection>(manager, network, queue);
		
		SimpleFIFOChannel channel = new SimpleFIFOChannel();

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
		
		serdes.shutdown();
		
		assertTrue("Shutdown caused bytes to be written", serdes.write(channel) == 0);
		
		serdes.writePacket(p);
		
		assertTrue("Network not in write request mode", network.getWriteRequest());
		
		serdes.write(channel);
		
		assertTrue("Network in write request mode", !network.getWriteRequest());	
		
		assertNull("Network processed packet after shutdown", channel.read());
		
	}
	
	@Test
	public void passthroughTest() throws IOException {
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		SimpleNetwork network = new SimpleNetwork();

		StripedQueue<Packet<SimpleConnection>> queue = new SimpleStripedQueue();

		Serdes<SimpleConnection> serdesEncoder = new SerdesImpl<SimpleConnection>(manager, network, null);
		
		Serdes<SimpleConnection> serdesDecoder = new SerdesImpl<SimpleConnection>(manager, null, queue);

		SimpleFIFOChannel channel = new SimpleFIFOChannel();
		
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

	private void writeIntPacket(SimpleFIFOChannel channel, int i) {
		channel.write(0xAA, 0x55, 0x00, 0x00, i >> 24, i >> 16, i >> 8, i);
	}
	
	@SuppressWarnings("unused")
	private void writeLongPacket(SimpleFIFOChannel channel, long l) {
		channel.write(0xAAL, 0x55L, 0x00L, 0x01L, l >> 56, l >> 48, l >> 40, l >> 32, l >> 24, l >> 16, l >> 8, l);
	}
	
	public void checkIntPacket(SimpleFIFOChannel channel, int i) {
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0xAA);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x55);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x00);
		assertEquals("Packet header encode error", (byte)(int) channel.read(), (byte) 0x00);
		
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 24) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 16) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 8) & 0xFF));
		assertEquals("Packet data encode error", (byte)(int) channel.read(), (byte) ((i >> 0) & 0xFF));
	}
	
	public void checkLongPacket(SimpleFIFOChannel channel, long l) {
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
	
	public GenericPacket getPacket(final Object data, SimpleProtocol protocol) {
		int type = (data instanceof Long) ? 1 : 0;
		return protocol.new GenericPacket(type) {
			@Override
			public Object getData() {
				return data;
			}
		};
	}

}
