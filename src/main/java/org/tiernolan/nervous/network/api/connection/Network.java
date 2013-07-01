package org.tiernolan.nervous.network.api.connection;

import org.tiernolan.nervous.network.api.protocol.Packet;

public interface Network<C extends Connection<C>> {
	/**
	 * Writes a packet to the Network
	 * 
	 * @param p
	 */
	public void writePacket(Packet<C> p);

	/**
	 * Shuts down the channel cleanly
	 */
	public void shutdown();
}
