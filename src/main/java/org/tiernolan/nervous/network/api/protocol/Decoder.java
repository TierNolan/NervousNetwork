package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

import org.tiernolan.nervous.network.api.connection.Connection;

public interface Decoder<P extends Packet<C>, C extends Connection<C>> extends ProtocolComponent<C> {

	/**
	 * Decodes a packet from the packet header and body
	 * 
	 * @param header a ByteBuffer containing the header
	 * @param body a ByteBuffer containing the body
	 * @return the decoded packet, or null on failure
	 */
	public Packet<C> decode(ByteBuffer header, ByteBuffer body);

}
