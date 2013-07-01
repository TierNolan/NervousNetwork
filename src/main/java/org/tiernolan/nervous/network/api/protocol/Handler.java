package org.tiernolan.nervous.network.api.protocol;

import org.tiernolan.nervous.network.api.connection.Connection;


public interface Handler<P extends Packet<C>, C extends Connection<C>> extends ProtocolComponent<C> {

	/**
	 * Handles a packet.
	 * 
	 * @param connection the connection
	 * @param packet the packet
	 */
	public void handle(Connection<C> connection, P packet);

}
