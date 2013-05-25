package org.tiernolan.nervous.network.api.queue;

public interface Striped {
	
	/**
	 * Gets the stripe of the object.<br>
	 * 
	 * @return the stripe, or -1 for asynchronous
	 */
	public int getStripeId();

}
