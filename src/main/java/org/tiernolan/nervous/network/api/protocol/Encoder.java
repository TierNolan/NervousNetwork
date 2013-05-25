package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

public interface Encoder<T extends Packet> extends ProtocolComponent {
	
	/**
	 * Encodes a packet to a header and body
	 * 
	 * @param connection the connection
	 * @param packet the packet
	 * @param buf a ByteBuffer for the packet
	 */
	public void encode(T packet, ByteBuffer buf);
	
}
