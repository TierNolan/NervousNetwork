package org.tiernolan.nervous.network.queue;

import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.queue.Striped;

public class PacketWrapper<C extends Connection<C>> implements Striped {
	
	private final C connection;
	private final Packet<C> packet;
	
	public PacketWrapper(C connection, Packet<C> packet) {
		this.connection = connection;
		this.packet = packet;
	}
	
	public Packet<C> getPacket() {
		return packet;
	}
	
	public C getConnection() {
		return connection;
	}

	public int getStripeId() {
		return packet.getStripeId();
	}
	
	public String toString() {
		return "{ " + connection + ", " + packet + "}";
	}

}
