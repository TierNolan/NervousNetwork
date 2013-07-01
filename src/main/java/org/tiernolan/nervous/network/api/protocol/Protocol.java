package org.tiernolan.nervous.network.api.protocol;

import java.nio.ByteBuffer;

import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.connection.Network;

public interface Protocol<C extends Connection<C>> {

	/**
	 * Gets the packet header size for this protocol
	 * 
	 * @return the packet header size
	 */
	public int getPacketHeaderSize();
	
	/**
	 * Gets the maximum packet size for this protocol
	 * 
	 * @return the maximum packet size
	 */
	public int getMaxPacketSize();
	
	/**
	 * Seeks to the start of a message.  
	 * 
	 * If the first byte in the buffer is not the start of a header, seek to the start of a header.
	 * 
	 * @param header a ByteBuffer containing the header
	 * @return true if a header start was found
	 */
	public boolean seekToHeader(ByteBuffer header);
	
	/**
	 * Gets the size of the packet body from a packet header.
	 * 
	 * @param header a ByteBuffer containing the packet header
	 * @return the packet body size, or -1 if the header is incomplete
	 */
	public int getPacketBodySize(ByteBuffer header);
	
	/**
	 * Get the packet decoder from a packet header.
	 * 
	 * @param header a ByteBuffer containing the packet header
	 * @return the decoder, or null if the header is incomplete
	 */
	public <P extends Packet<C>> Decoder<P, C> getPacketDecoder(ByteBuffer header);
	
	/**
	 * Get the packet decoder from a packet header.
	 * 
	 * @param packet the packet
	 * @return the decoder, or null if the header is incomplete
	 */
	public <P extends Packet<C>> Encoder<P, C> getPacketEncoder(Packet<C> packet);
	
	/**
	 * Get the packet decoder from a packet header.
	 * 
	 * @param header the packet
	 * @return the handler
	 */
	public <P extends Packet<C>> Handler<P, C> getPacketHandler(Packet<C> packet);
	
	/**
	 * Gets a connection instance associated with this protocol
	 * 
	 * @return the connection
	 */
	public C newConnection(Network<C> network);
	
}
