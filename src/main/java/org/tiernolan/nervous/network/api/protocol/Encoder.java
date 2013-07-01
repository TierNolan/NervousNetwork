package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

import org.tiernolan.nervous.network.api.connection.Connection;

public interface Encoder<P extends Packet<C>, C extends Connection<C>> extends ProtocolComponent<C> {
	
	/**
	 * Encodes a packet to a header and body
	 * 
	 * @param connection the connection
	 * @param packet the packet
	 * @param buf a ByteBuffer for the packet
	 */
	public void encode(P packet, ByteBuffer buf);
	
	/**
	 * Gets the packet body size
	 * 
	 * @param packet
	 * @return
	 */
	public int getPacketBodySize(P packet);
	
}
