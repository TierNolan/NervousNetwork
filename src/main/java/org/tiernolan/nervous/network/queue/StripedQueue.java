package org.tiernolan.nervous.network.queue;

import java.util.HashMap;
import java.util.LinkedList;

import org.tiernolan.nervous.network.api.queue.Striped;


public class StripedQueue<T extends Striped> {
	
	private final HashMap<Integer, LinkedList<CompletableStriped<T>>> stripes = new  HashMap<Integer, LinkedList<CompletableStriped<T>>>();
	
	private final LinkedList<CompletableStriped<T>> asyncQueue = new LinkedList<CompletableStriped<T>>();
	private final LinkedList<LinkedList<CompletableStriped<T>>> queueQueue = new LinkedList<LinkedList<CompletableStriped<T>>>();

	public synchronized boolean offer(T e) {
		try {
			LinkedList<CompletableStriped<T>> queue = getQueue(e.getStripeId());
			return queue.offer(new Completable(e, queue));
		} finally {
			notify();
		}
	}

	public synchronized CompletableStriped<T> poll() {
		CompletableStriped<T> o = asyncQueue.poll();
		if (o != null) {
			return o;
		}
		LinkedList<CompletableStriped<T>> l;
		
		while ((l = queueQueue.poll()) != null) {
			o = l.poll();
			if (o != null) {
				return o;
			}
		}
		
		return null;
	}
	
	public synchronized boolean isEmpty() {
		if (!asyncQueue.isEmpty()) {
			return false;
		}
		for (LinkedList<CompletableStriped<T>> stripeQueue : stripes.values()) {
			if (!stripeQueue.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	public synchronized CompletableStriped<T> take() throws InterruptedException {
		while (true) {
			CompletableStriped<T> o = poll();
			
			if (o != null) {
				return o;
			}
			wait();
		}
	}

	private synchronized LinkedList<CompletableStriped<T>> getQueue(int stripeId) {
		
		if (stripeId == -1) {
			return asyncQueue;
		}
		
		LinkedList<CompletableStriped<T>> queue = stripes.get(stripeId);
		
		if (queue == null) {
			queue = new LinkedList<CompletableStriped<T>>();
			stripes.put(stripeId, queue);
			
			queueQueue.add(queue);
		}
		
		return queue;
		
	}
	
	private class Completable implements CompletableStriped<T> {
		
		private final LinkedList<CompletableStriped<T>> queue;
		private final int stripeId;
		private final T striped;
		
		public Completable(T striped, LinkedList<CompletableStriped<T>> queue) {
			this.striped = striped;
			this.stripeId = striped.getStripeId();
			this.queue = queue;
		}

		public int getStripeId() {
			return stripeId;
		}

		public void done() {
			synchronized (StripedQueue.this) {
				if (queue == asyncQueue) {
					return;
				} else if (!queue.isEmpty()) {
					queueQueue.add(queue);
					StripedQueue.this.notify();
					
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
