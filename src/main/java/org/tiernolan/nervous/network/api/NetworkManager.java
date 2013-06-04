package org.tiernolan.nervous.network.api;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface NetworkManager extends ProtocolComponent {

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
	
}
