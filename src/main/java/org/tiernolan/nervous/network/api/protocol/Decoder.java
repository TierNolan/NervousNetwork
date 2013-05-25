package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

public interface Decoder<T extends Packet> extends ProtocolComponent {

	/**
	 * Decodes a packet from the packet header and body
	 * 
	 * @param header a ByteBuffer containing the header
	 * @param body a ByteBuffer containing the body
	 * @return the decoded packet, or null on failure
	 */
	public T decode(ByteBuffer header, ByteBuffer body);

}
