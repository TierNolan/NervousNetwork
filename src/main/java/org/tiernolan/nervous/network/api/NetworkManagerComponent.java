package org.tiernolan.nervous.network.api;

import org.tiernolan.nervous.network.api.connection.Connection;

public interface NetworkManagerComponent<C extends Connection<C>> {
	
	/**
	 * Gets the associated network manager
	 * 
	 * @return the network manager
	 */
	public NetworkManager<C> getNetworkManager();

}
