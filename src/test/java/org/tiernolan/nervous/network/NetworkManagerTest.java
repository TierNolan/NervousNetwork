package org.tiernolan.nervous.network;

import static org.junit.Assert.assertEquals;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;

import org.junit.Test;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.connection.SimpleConnection;
import org.tiernolan.nervous.network.connection.SimpleProtocol;
import org.tiernolan.nervous.network.connection.SimpleProtocol.GenericPacket;
import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.StripedMergingQueue;

public class NetworkManagerTest {
	
	@Test
	public void test() throws IOException, InterruptedException {
		
		SimpleProtocol protocol = new SimpleProtocol();
		
		NetworkManagerImpl<SimpleConnection> manager = new NetworkManagerImpl<SimpleConnection>(protocol);
		
		manager.listen(1234);
		
		Socket s = new Socket("localhost", 1234);
		
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		
		writeIntPacket(dos, 0x77);
		
		StripedMergingQueue<Packet<SimpleConnection>> q = getMasterQueue(manager);
		
		Completable<Packet<SimpleConnection>> c = q.take();
		
		GenericPacket p = (GenericPacket) c.getStriped();
		
		assertEquals("Packet value does not match expected", p.getData(), 0x77);
		
		manager.shutdown();
		
	}
	
	@SuppressWarnings("unchecked")
	private StripedMergingQueue<Packet<SimpleConnection>> getMasterQueue(NetworkManagerImpl<SimpleConnection> manager) {
		Field f;
		try {
			f = NetworkManagerImpl.class.getDeclaredField("masterQueue");
			f.setAccessible(true);
			return (StripedMergingQueue<Packet<SimpleConnection>>) f.get(manager);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private void writeIntPacket(DataOutputStream dos, int i) throws IOException {
		dos.write(new byte[] {(byte) 0xAA, (byte) 0x55, (byte) 0x00, (byte) 0x00, (byte) (i >> 24), (byte) (i >> 16), (byte) (i >> 8), (byte) i});
		dos.flush();
	}
	
	@SuppressWarnings("unused")
	private void writeLongPacket(DataOutputStream dos, long l) throws IOException {
		dos.write(new byte[] {(byte) 0xAAL, (byte) 0x55L, (byte) 0x00L, (byte) 0x01L, (byte) (l >> 56), (byte) (l >> 48), (byte) (l >> 40), (byte) (l >> 32), 
				(byte) (l >> 24), (byte) (l >> 16), (byte) (l >> 8), (byte) l});
		dos.flush();
	}

}
