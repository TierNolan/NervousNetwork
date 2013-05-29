package org.tiernolan.nervous.network.queue;

import org.tiernolan.nervous.network.api.queue.Striped;


public interface StripedQueue <T extends Striped> {
	
	/**
	 * Inserts the element into the queue
	 * 
	 * @param e
	 * @return true if successful
	 */
	public boolean offer(T e);
	
	/**
	 * Retrieves the head of the queue without removal
	 * 
	 * @return the head of the queue or null, if empty
	 */
	public T peek();
	
	/**
	 * Removes the head of the queue and gets the associated CompletableStriped
	 * 
	 * @return the head of the queue or null, if empty
	 */
	public Completable<T> poll();
	
	/**
	 * Removes the head of the queue and gets the associated CompletableStriped, waiting if necessary
	 * 
	 * @return the head of the queue
	 * @throws InterruptedException
	 */
	public Completable<T> take() throws InterruptedException;
	
	/**
	 * Returns true if there are no elements in the queue.
	 * 
	 * @return
	 */
	public boolean isEmpty();
}
