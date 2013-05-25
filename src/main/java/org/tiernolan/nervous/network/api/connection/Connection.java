package org.tiernolan.nervous.network.api.connection;

import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface Connection extends ProtocolComponent {

	/**
	 * Closes the connection.  This method has no effect if 
	 * the connection is already closed
	 */
	public void close();
	
}
