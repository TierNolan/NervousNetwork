package org.tiernolan.nervous.network.api.protocol;

import org.tiernolan.nervous.network.api.connection.Connection;


public interface Handler<T extends Packet> extends ProtocolComponent {

	/**
	 * Handles a packet.
	 * 
	 * Packets for a given connection are handled serially.
	 * 
	 * @param connection the connection
	 * @param packet the packet
	 */
	public void handle(Connection connection, T packet);

}
