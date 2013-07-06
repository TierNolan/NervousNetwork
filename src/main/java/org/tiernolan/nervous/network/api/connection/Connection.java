package org.tiernolan.nervous.network.api.connection;

import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public abstract class Connection<C extends Connection<C>> implements ProtocolComponent<C>, NetworkComponent<C> {
	
	protected Network<C> network;
	
	protected Connection(Network<C> network) {
		this.network = network;
	}
	
	/**
	 * Shuts down the connection cleanly
	 */
	public void shutdown() {
		getNetwork().shutdown();
	}
	
	/**
	 * Gets the Network associated with this connection
	 */
	public Network<C> getNetwork() {
		return network;
	}
	
}
