package org.tiernolan.nervous.network.api.connection;


public interface NetworkComponent<C extends Connection<C>> {
	
	/**
	 * Gets the associated network
	 * 
	 * @return the network
	 */
	public Network<C> getNetwork();

}
