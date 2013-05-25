package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

import org.tiernolan.nervous.network.api.connection.Connection;

public interface Encoder<T extends Packet> extends ProtocolComponent {
	
	/**
	 * Encodes a packet to a header and body
	 * 
	 * @param connection the connection
	 * @param packet the packet
	 * @param header a ByteBuffer for the header
	 * @param body a ByteBuffer for the body
	 */
	public void encode(Connection connection, T packet, ByteBuffer header, ByteBuffer body);
	
}
