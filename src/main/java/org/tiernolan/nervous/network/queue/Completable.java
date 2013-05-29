package org.tiernolan.nervous.network.queue;

import org.tiernolan.nervous.network.api.queue.Striped;

public interface Completable<T extends Striped> extends Striped {
	
	/**
	 * Indicates that this object is completed and the next object can be read from the queue
	 */
	public void done();
	
	/**
	 * Gets the Striped object
	 */
	public T getStriped();

}
