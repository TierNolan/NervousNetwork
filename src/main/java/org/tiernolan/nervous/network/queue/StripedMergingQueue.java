package org.tiernolan.nervous.network.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.tiernolan.nervous.network.api.queue.Striped;

public class StripedMergingQueue<T extends Striped> implements StripedQueue<T> {
	
	protected ConcurrentLinkedQueue<StripedQueueImpl<T>> masterQueue = new ConcurrentLinkedQueue<StripedQueueImpl<T>>();
	
	private final Lock lock = new ReentrantLock();
	
	protected void notify(StripedQueueImpl<T> slave) {
		masterQueue.add(slave);
		notifyOne();
	}
	
	protected void notifyOne() {
		synchronized (this) {
			notify();
		}
	}

	public boolean offer(T e) {
		throw new UnsupportedOperationException("Striped objects must be added to slave queues");
	}

	public T peek() {
		for (StripedQueueImpl<T> q : masterQueue) {
			T s = q.peekRaw();
			if (s != null) {
				return s;
			}
		}
		return null;
	}

	public Completable<T> poll() {
		StripedQueueImpl<T> q;
		
		while ((q = masterQueue.peek()) != null) {
			Completable<T> s = q.pollRaw();
			if (s != null) {
				return s;
			}
			pollIfNextAndEmpty(q);
		}
		return null;
	}
	
	public Completable<T> take() throws InterruptedException {
		Completable<T> s = poll();
		if (s != null) {
			return s;
		}
		synchronized(this) {
			while (true) {
				s = poll();
				if (s != null) {
					return s;
				}
				wait();
			}
		}
	}
	
	private boolean pollIfNextAndEmpty(StripedQueueImpl<T> q) {
		lock.lock();
		try {
			if (masterQueue.peek() == q) {
				if (q.notifyUnqueued()) {
					masterQueue.poll();
					return true;
				}
			}
			return false;
		} finally {
			lock.unlock();
		}
	}

	public boolean isEmpty() {
		for (StripedQueueImpl<T> q : masterQueue) {
			if (!q.isEmptyRaw()) {
				return false;
			}
		}
		return true;
	}

}
