package org.tiernolan.nervous.network.api.protocol;

import org.tiernolan.nervous.network.api.queue.Striped;

public interface Packet extends ProtocolComponent, Striped {
	
	/**
	 * Gets the stripe of the packet.<br>
	 * <br>
	 * Packets with the same stripe id are handled serially.
	 * 
	 * @return the stripe, or -1 for asynchronous
	 */
	public int getStripeId();

}
