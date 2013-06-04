package org.tiernolan.nervous.network.queue;

import java.util.HashMap;
import java.util.LinkedList;

import org.tiernolan.nervous.network.api.queue.Striped;


public class StripedQueueImpl<T extends Striped> implements StripedQueue<T> {
	
	private final HashMap<Integer, LinkedList<Completable<T>>> stripes = new HashMap<Integer, LinkedList<Completable<T>>>();
	
	private final LinkedList<Completable<T>> asyncQueue = new LinkedList<Completable<T>>();
	private final LinkedList<LinkedList<Completable<T>>> queueQueue = new LinkedList<LinkedList<Completable<T>>>();

	public synchronized boolean offer(T e) {
		try {
			LinkedList<Completable<T>> queue = getQueue(e.getStripeId());
			return queue.offer(new CompletableImpl(e, queue));
		} finally {
			notify();
		}
	}
	
	public synchronized T peek() {
		Completable<T> o = asyncQueue.peek();
		if (o != null) {
			return o.getStriped();
		}

		for (LinkedList<Completable<T>> l : queueQueue) {
			o = l.peek();
			if (o != null) {
				return o.getStriped();
			}
		}
		
		return null;
	}
	
	public synchronized Completable<T> poll() {
		Completable<T> o = asyncQueue.poll();
		if (o != null) {
			return o;
		}
		LinkedList<Completable<T>> l;
		
		while ((l = queueQueue.poll()) != null) {
			o = l.poll();
			if (o != null) {
				return o;
			}
		}
		
		return null;
	}
	
	public synchronized Completable<T> take() throws InterruptedException {
		while (true) {
			Completable<T> o = poll();
			
			if (o != null) {
				return o;
			}
			wait();
		}
	}
	
	public synchronized boolean isEmpty() {
		if (!asyncQueue.isEmpty()) {
			return false;
		}
		for (LinkedList<Completable<T>> stripeQueue : stripes.values()) {
			if (!stripeQueue.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	private synchronized LinkedList<Completable<T>> getQueue(int stripeId) {
		
		if (stripeId == -1) {
			return asyncQueue;
		}
		
		LinkedList<Completable<T>> queue = stripes.get(stripeId);
		
		if (queue == null) {
			queue = new LinkedList<Completable<T>>();
			stripes.put(stripeId, queue);
			
			queueQueue.add(queue);
		}
		
		return queue;
		
	}
	
	private class CompletableImpl implements Completable<T> {
		
		private final LinkedList<Completable<T>> queue;
		private final int stripeId;
		private final T striped;
		
		public CompletableImpl(T striped, LinkedList<Completable<T>> queue) {
			this.striped = striped;
			this.stripeId = striped.getStripeId();
			this.queue = queue;
		}

		public int getStripeId() {
			return stripeId;
		}

		public void done() {
			synchronized (StripedQueueImpl.this) {
				if (queue == asyncQueue) {
					return;
				} else if (!queue.isEmpty()) {
					queueQueue.add(queue);
					StripedQueueImpl.this.notify();
					
				} else {
					if (stripes.remove(stripeId) != queue) {
						throw new IllegalStateException("Queue was not removed correctly in StripedQueue");
					}
				}
			}
		}

		public T getStriped() {
			return striped;
		}
		
	}
	
}
