package org.tiernolan.nervous.network.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface NetworkManager<C extends Connection<C>> extends ProtocolComponent<C> {

	/**
	 * Gets a Logger object
	 * 
	 * @return
	 */
	public Logger getLogger();
	
	/**
	 * Gets the executor service
	 * 
	 * @return
	 */
	public ExecutorService getExecutorService();
	
	/**
	 * Listens on the given port for incoming connections
	 * 
	 * @param port
	 */
	public void listen(int port) throws IOException;
	
	/**
	 * Listens on the given address for incoming connections
	 * 
	 * @param addr
	 */
	public void listen(InetSocketAddress addr) throws IOException;
	
}
