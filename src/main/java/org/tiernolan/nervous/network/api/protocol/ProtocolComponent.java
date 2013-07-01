package org.tiernolan.nervous.network.api.protocol;

import org.tiernolan.nervous.network.api.connection.Connection;

public interface ProtocolComponent<C extends Connection<C>> {
	
	/**
	 * Gets the associated protocol
	 * 
	 * @return the protocol
	 */
	public Protocol<C> getProtocol();

}
