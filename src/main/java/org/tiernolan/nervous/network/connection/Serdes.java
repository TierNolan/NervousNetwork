package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.tiernolan.nervous.network.api.NetworkManagerComponent;
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.connection.Network;
import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface Serdes<C extends Connection<C>> extends Network<C>, ProtocolComponent<C>, NetworkManagerComponent<C> {

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
	
}
