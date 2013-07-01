package org.tiernolan.nervous.network.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Random;

import org.junit.Test;
import org.tiernolan.nervous.network.NetworkManagerImpl;
import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.connection.SimpleProtocol.GenericPacket;
import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.StripedQueue;
import org.tiernolan.nervous.network.queue.StripedQueueImpl;

public class SelectorTest {
	
	private final int port = 12345;
	
	@Test
	public void decodeTest() throws IOException, InterruptedException {
		
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		StripedQueue<Packet<SimpleConnection>> queue = new StripedQueueImpl<Packet<SimpleConnection>>();
		
		SelectorHandler<SimpleConnection> selectorHandler = new SelectorHandler<SimpleConnection>(manager);

		selectorHandler.start();
		
		ServerSocketChannel server = ServerSocketChannel.open();
		
		server.bind(new InetSocketAddress(port));
		
		SocketChannel out = SocketChannel.open();
		out.connect(new InetSocketAddress(port));
		
		SocketChannel in = server.accept();
		
		selectorHandler.addChannel(in, queue);
		
		writeIntPacket(out, 42);
		
		Packet<SimpleConnection> p = queue.take().getStriped();
		
		assertEquals("Packet decode failure", ((GenericPacket) p).getData(), 42);
		
		selectorHandler.interrupt();
		
		selectorHandler.join(1000);
		
		assertTrue("Selector handler thread alive", !selectorHandler.isAlive());

		assertTrue("Input channel did not automatically close", !in.isOpen());
		
		out.close();
		
		server.close();
	}
	
	@Test
	public void encodeTest() throws IOException, InterruptedException {
		
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		StripedQueue<Packet<SimpleConnection>> queue = new StripedQueueImpl<Packet<SimpleConnection>>();
		
		SelectorHandler<SimpleConnection> selectorHandler = new SelectorHandler<SimpleConnection>(manager);

		selectorHandler.start();
		
		ServerSocketChannel server = ServerSocketChannel.open();
		
		server.bind(new InetSocketAddress(port));
		
		SocketChannel out = SocketChannel.open();
		out.connect(new InetSocketAddress(port));
		
		SocketChannel in = server.accept();
		
		ChannelHandler<SimpleConnection> handler = selectorHandler.addChannel(in, queue);

		Serdes<SimpleConnection> serdes = handler.getSerdes();
		
		GenericPacket p = getPacket(7, protocol);

		serdes.writePacket(p);
		
		checkIntPacket(out, 7);
		
		selectorHandler.interrupt();
		
		selectorHandler.join(1000);
		
		assertTrue("Selector handler thread alive", !selectorHandler.isAlive());

		assertTrue("Input channel did not automatically close", !in.isOpen());
		
		out.close();
		
		server.close();
	}
	
	@Test
	public void shutdown() throws IOException, InterruptedException {
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		StripedQueue<Packet<SimpleConnection>> queue = new StripedQueueImpl<Packet<SimpleConnection>>();
		
		SelectorHandler<SimpleConnection> selectorHandler = new SelectorHandler<SimpleConnection>(manager);

		selectorHandler.start();
		
		ServerSocketChannel server = ServerSocketChannel.open();
		
		server.bind(new InetSocketAddress(port));
		
		SocketChannel out = SocketChannel.open();
		out.connect(new InetSocketAddress(port));
		
		SocketChannel in = server.accept();
		
		ChannelHandler<SimpleConnection> handler = selectorHandler.addChannel(in, queue);

		Serdes<SimpleConnection> serdes = handler.getSerdes();
		
		GenericPacket p = getPacket(7, protocol);
		
		for (int i = 0; i < 16384; i++) {
			serdes.writePacket(p);
		}
		
		selectorHandler.interrupt();
		
		selectorHandler.join(100);
		
		assertTrue("Selector handler thread not alive", selectorHandler.isAlive());
		
		long start = System.currentTimeMillis();
		selectorHandler.shutdown(100L);
		
		selectorHandler.join(200);
		
		assertTrue("Selector did not wait at least 100ms before shutting down", (System.currentTimeMillis() - start) >= 100);

		assertTrue("Input channel did not automatically close", !in.isOpen());
		
		out.close();
		
		server.close();
	}
	
	@Test
	public void randomDecodeTest() throws IOException, InterruptedException {
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		StripedQueue<Packet<SimpleConnection>> queue = new StripedQueueImpl<Packet<SimpleConnection>>();
		
		SelectorHandler<SimpleConnection> selectorHandler = new SelectorHandler<SimpleConnection>(manager);

		selectorHandler.start();
		
		ServerSocketChannel server = ServerSocketChannel.open();
		
		server.bind(new InetSocketAddress(port));
		
		SocketChannel out = SocketChannel.open();
		out.connect(new InetSocketAddress(port));
		
		SocketChannel in = server.accept();
		
		selectorHandler.addChannel(in, queue);

		Random r = new Random();
		
		int[] values = new int[1000];
		
		for (int i = 0; i < values.length; i++) {
			int v = r.nextInt();
			writeIntPacket(out, v);
			values[i] = v;
		}
		
		for (int i = 0; i < values.length; i++) {
			Completable<Packet<SimpleConnection>> c = queue.take();
			Packet<SimpleConnection> p = (Packet<SimpleConnection>) c.getStriped();
			assertEquals("Packet decode failure", ((GenericPacket) p).getData(), values[i]);
			c.done();
		}
		
		assertTrue("Unexpected packet decoded", queue.poll() == null);
		
		selectorHandler.interrupt();
		
		selectorHandler.join(100);
		
		assertTrue("Selector handler thread alive", !selectorHandler.isAlive());
		
		assertTrue("Input channel did not automatically close", !in.isOpen());
		
		out.close();
		
		server.close();
	}
	
	@Test
	public void randomEncodeTest() throws IOException, InterruptedException {
		SimpleProtocol protocol = new SimpleProtocol();

		NetworkManager<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);

		StripedQueue<Packet<SimpleConnection>> queue = new StripedQueueImpl<Packet<SimpleConnection>>();
		
		SelectorHandler<SimpleConnection> selectorHandler = new SelectorHandler<SimpleConnection>(manager);

		selectorHandler.start();
		
		ServerSocketChannel server = ServerSocketChannel.open();
		
		server.bind(new InetSocketAddress(port));
		
		SocketChannel out = SocketChannel.open();
		out.connect(new InetSocketAddress(port));
		
		SocketChannel in = server.accept();
		
		ChannelHandler<SimpleConnection> handler = selectorHandler.addChannel(in, queue);
		
		Serdes<SimpleConnection> serdes = handler.getSerdes();

		Random r = new Random();
		
		Object[] values = new Object[500];
		
		for (int i = 0; i < values.length; i++) {
			Object data;
			if (r.nextBoolean()) {
				data = (Integer) r.nextInt();
			} else {
				data = (Long) r.nextLong();
			}
			GenericPacket p = getPacket(data, protocol);
			serdes.writePacket(p);
			values[i] = data;
		}
		
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Long) {
				checkLongPacket(out, (Long) values[i]);
			} else {
				checkIntPacket(out, (Integer) values[i]);
			}
		}
		
		assertTrue("Unexpected packet decoded", queue.poll() == null);
		
		selectorHandler.interrupt();
		
		selectorHandler.join(100);
		
		assertTrue("Selector handler thread alive", !selectorHandler.isAlive());
		
		assertTrue("Input channel did not automatically close", !in.isOpen());
		
		out.close();
		
		server.close();
	}
	
	private void writeIntPacket(SocketChannel channel, int i) throws IOException {
		channelWrite(channel, 0xAA, 0x55, 0x00, 0x00, i >> 24, i >> 16, i >> 8, i);
	}
	
	@SuppressWarnings("unused")
	private void writeLongPacket(SimpleFIFOChannel channel, long l) throws IOException {
		channelWrite(channel, 0xAAL, 0x55L, 0x00L, 0x01L, l >> 56, l >> 48, l >> 40, l >> 32, l >> 24, l >> 16, l >> 8, l);
	}
	
	private void channelWrite(WritableByteChannel channel, Long ... longs) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(longs.length);
		for (long l : longs) {
			buf.put((byte) l);
		}
		buf.flip();
		channel.write(buf);
	}
	
	private void channelWrite(WritableByteChannel channel, Integer ... ints) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(ints.length);
		for (int i : ints) {
			buf.put((byte) i);
		}
		buf.flip();
		channel.write(buf);
	}
	
	public void checkIntPacket(ReadableByteChannel channel, int i) throws IOException, InterruptedException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		
		while (buf.hasRemaining()) {
			channel.read(buf);
			if (buf.hasRemaining()) {
				Thread.sleep(5);
			}
		}
		buf.flip();
		
		assertEquals("Packet header encode error", buf.getInt(), 0xAA550000);
		
		buf.clear();
		while (buf.hasRemaining()) {
			channel.read(buf);
			if (buf.hasRemaining()) {
				Thread.sleep(5);
			}
		}
		buf.flip();
		
		long data = buf.getInt();
		
		assertEquals("Packet data encode error", data, i);
	}
	
	public void checkLongPacket(ReadableByteChannel channel, long l) throws IOException, InterruptedException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		
		while (buf.hasRemaining()) {
			channel.read(buf);
			if (buf.hasRemaining()) {
				Thread.sleep(5);
			}
		}
		buf.flip();
		
		assertEquals("Packet header encode error", buf.getInt(), 0xAA550001);
		
		buf = ByteBuffer.allocate(8);
		
		while (buf.hasRemaining()) {
			channel.read(buf);
			if (buf.hasRemaining()) {
				Thread.sleep(5);
			}	
		}
		buf.flip();
		
		long data = buf.getLong();
		
		assertEquals("Packet data encode error", data, l);
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
