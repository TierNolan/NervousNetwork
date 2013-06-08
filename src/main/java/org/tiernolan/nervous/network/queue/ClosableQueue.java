package org.tiernolan.nervous.network.queue;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClosableQueue<T> implements Iterable<T> {

	private final int id;
	private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();
	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();
	private boolean closed = false;
	
	public ClosableQueue(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean offer(T e) {
		readLock.lock();
		try {
			if (closed) {
				return false;
			}
			return queue.offer(e);
		} finally {
			readLock.unlock();
		}
	}
	
	public T peek() {
		return queue.peek();
	}

	public Iterator<T> iterator() {
		return new UnmodifiableIterator<T>(queue.iterator());
	}
	
	public T poll() {
		return queue.poll();
	}
	
	public boolean isEmpty() {
		return queue.isEmpty();
	}
	
	public boolean close() {
		writeLock.lock();
		try {
			if (closed) {
				return false;
			}
			if (queue.isEmpty()) {
				closed = true;
			}
			return closed;
		} finally {
			writeLock.unlock();
		}
	}
	
	private static class UnmodifiableIterator<T> implements Iterator<T> {

		private final Iterator<T> i;
		
		public UnmodifiableIterator(Iterator<T> i) {
			this.i = i;
		}
		
		public boolean hasNext() {
			return i.hasNext();
		}

		public T next() {
			return i.next();
		}

		public void remove() {
			throw new UnsupportedOperationException("Modification is not supported");
		}
		
	}

}
