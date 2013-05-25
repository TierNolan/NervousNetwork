package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

import org.tiernolan.nervous.network.api.connection.Connection;

public interface Decoder<T extends Packet> extends ProtocolComponent {

	/**
	 * Decodes a packet from the packet header and body
	 * 
	 * @param connection the connection
	 * @param header a ByteBuffer containing the header
	 * @param body a ByteBuffer containing the body
	 * @return the decoded packet, or null on failure
	 */
	public T decode(Connection connection, ByteBuffer header, ByteBuffer body);

}
