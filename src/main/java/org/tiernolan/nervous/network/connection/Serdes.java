package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.tiernolan.nervous.network.api.NetworkComponent;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface Serdes extends ProtocolComponent, NetworkComponent{

	/**
	 * Called by the Network when new data arrives.  The ByteStream is converted into Packets
	 * 
	 * @param channel
	 * @return the number of bytes processed
	 * @throws IOException
	 */
	public int read(ReadableByteChannel channel) throws IOException;
	
	/**
	 * Called by the Network to write packets to the network channel.
	 * 
	 * @param channel
	 * @return the number of bytes written
	 * @throws IOException
	 */
	public int write(WritableByteChannel channel) throws IOException;
	
	/**
	 * Writes a packet to the network
	 * 
	 * @param p
	 */
	public void writePacket(Packet p);
	
}
