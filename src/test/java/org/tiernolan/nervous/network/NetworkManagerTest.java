package org.tiernolan.nervous.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.tiernolan.nervous.network.connection.SimpleConnection;
import org.tiernolan.nervous.network.connection.SimpleProtocol;

public class NetworkManagerTest {
	
	@Test
	public void test() throws IOException, InterruptedException {
		
		SimpleProtocol protocol = new SimpleProtocol();
		
		NetworkManagerImpl<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);
		
		manager.listen(1234);
		
		Socket s = new Socket("localhost", 1234);
		
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		DataInputStream dis = new DataInputStream(s.getInputStream());

		Thread t = setTimeout(manager, 1000);
		
		writeIntPacket(dos, 0x77);
		
		checkIntPacket(dis, 0x77);
		
		writeLongPacket(dos, 0x1234567L);
		
		checkLongPacket(dis, 0x1234568L);
		
		writeIntPacket(dos, -1);
		
		checkEOF(dis);
		
		t.interrupt();
		manager.shutdown();
		
	}
	
	@Test
	public void randomTest() throws IOException, InterruptedException {
		
		Random r = new Random();
		
		final Object[] data = new Object[1000];
		
		for (int i = 0; i < data.length; i++) {
			if (r.nextBoolean()) {
				data[i] = r.nextInt();
				if (Integer.valueOf(-1).equals(data[i])) {
					data[i] = 1;
				}
			} else {
				data[i] = r.nextLong();
			}
		}
		
		SimpleProtocol protocol = new SimpleProtocol();
		
		NetworkManagerImpl<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);
		
		manager.listen(1234);
		
		Socket s = new Socket("localhost", 1234);
		
		final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		final DataInputStream dis = new DataInputStream(s.getInputStream());

		Thread t = setTimeout(manager, 1000);
		
		final AtomicBoolean success = new AtomicBoolean(false);
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>(null);
		
		Thread reader = new Thread() {
			public void run() {
				try {
					for (int i = 0; i < 1000; i++) {
						if (Long.class.equals(data[i].getClass())) {
							checkLongPacket(dis, ((Long) data[i]) + 1);
						} else {
							checkIntPacket(dis, (Integer) data[i]);
						}
					}
					success.set(true);
				} catch (Throwable t) {
					exception.set(t);
					return;
				}
			}
		};
		
		reader.start();
		
		for (int i = 0; i < 1000; i++) {
			if (Long.class.equals(data[i].getClass())) {
				writeLongPacket(dos, (Long) data[i]);
			} else {
				writeIntPacket(dos, (Integer) data[i]);
			}
		}
		
		writeIntPacket(dos, -1);

		reader.join(1000);
		
		if (exception.get() != null) {
			throw new AssertionError(exception.get());
		}
		assertTrue("Reader thread was not successful", success.get());
		
		checkEOF(dis);
		
		t.interrupt();
		manager.shutdown();
		
	}
	
	@Test
	public void stripeTest() throws IOException, InterruptedException {
		
		Random r = new Random();
		
		final Object[] data = new Object[1000];
		
		for (int i = 0; i < data.length; i++) {
			if (r.nextBoolean()) {
				data[i] = r.nextInt();
				if (Integer.valueOf(-1).equals(data[i])) {
					data[i] = 1;
				}
			} else {
				data[i] = r.nextLong();
			}
		}
		
		SimpleProtocol protocol = new SimpleProtocol();
		
		NetworkManagerImpl<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);
		
		manager.listen(1234);
		
		Socket s = new Socket("localhost", 1234);
		
		final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		final DataInputStream dis = new DataInputStream(s.getInputStream());

		Thread t = setTimeout(manager, 1000);
		
		final AtomicBoolean success = new AtomicBoolean(false);
		final AtomicReference<Throwable> exception = new AtomicReference<Throwable>(null);
		
		Thread reader = new Thread() {
			public void run() {
				try {
					for (int i = 0; i < 1000; i++) {
						if (Long.class.equals(data[i].getClass())) {
							checkLongPacket(dis, ((Long) data[i]) + 1);
						} else {
							checkIntPacket(dis, (Integer) data[i]);
						}
					}
					success.set(true);
				} catch (Throwable t) {
					exception.set(t);
					return;
				}
			}
		};
		
		reader.start();
		
		for (int i = 0; i < 1000; i++) {
			if (Long.class.equals(data[i].getClass())) {
				writeLongPacket(dos, (Long) data[i]);
			} else {
				writeIntPacket(dos, (Integer) data[i]);
			}
			writeInt2Packet(dos, i);
		}
		
		writeIntPacket(dos, -1);

		reader.join(1000);
		
		if (exception.get() != null) {
			throw new AssertionError(exception.get());
		}
		assertTrue("Reader thread was not successful", success.get());
		
		checkEOF(dis);
		
		t.interrupt();
		manager.shutdown();
		
	}
	
	private void writeIntPacket(DataOutputStream dos, int i) throws IOException {
		dos.write(new byte[] {(byte) 0xAA, (byte) 0x55, (byte) 0x00, (byte) 0x00, (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i});
		dos.flush();
	}
	
	private void writeLongPacket(DataOutputStream dos, long l) throws IOException {
		dos.write(new byte[] {(byte) 0xAAL, (byte) 0x55L, (byte) 0x00L, (byte) 0x01L, (byte) (l >> 56), (byte) (l >> 48), (byte) (l >> 40), (byte) (l >> 32), 
				(byte) (l >> 24), (byte) (l >> 16), (byte) (l >> 8), (byte) l});
		dos.flush();
	}
	
	private void writeInt2Packet(DataOutputStream dos, int i) throws IOException {
		dos.write(new byte[] {(byte) 0xAA, (byte) 0x55, (byte) 0x00, (byte) 0x02, (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i});
		dos.flush();
	}
	
	public void checkIntPacket(DataInputStream dis, int i) throws IOException {
		assertEquals("Packet header encode error", 0xAA, dis.read());
		assertEquals("Packet header encode error", 0x55, dis.read());
		assertEquals("Packet header encode error", 0x00, dis.read());
		assertEquals("Packet header encode error", 0x00, dis.read());
		
		assertEquals("Packet data encode error", ((i >> 24) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((i >> 16) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((i >> 8) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((i >> 0) & 0xFF), dis.read());
	}
	
	public void checkLongPacket(DataInputStream dis, long l) throws IOException {
		assertEquals("Packet header encode error", 0xAA, dis.read());
		assertEquals("Packet header encode error", 0x55, dis.read());
		assertEquals("Packet header encode error", 0x00, dis.read());
		assertEquals("Packet header encode error", 0x01, dis.read());
		
		assertEquals("Packet data encode error", ((l >> 56) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 48) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 40) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 32) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 24) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 16) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 8) & 0xFF), dis.read());
		assertEquals("Packet data encode error", ((l >> 0) & 0xFF), dis.read());
	}
	
	private void checkEOF(DataInputStream dis) throws IOException {
		assertEquals("End of stream expected", -1, dis.read());
	}
	
	private Thread setTimeout(final NetworkManagerImpl<SimpleConnection> manager, final long timeout) {
		
		Thread t = new Thread() {
			public void run() {
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					return;
				}
				System.out.println("Timing out");
				manager.shutdown();
			}
		};
		t.start();
		return t;
		
	}
	
}
