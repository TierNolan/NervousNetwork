package org.tiernolan.nervous.network.queue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tiernolan.nervous.network.api.queue.Striped;


public class StripedQueueImpl<T extends Striped> implements StripedQueue<T> {
	
	private final ConcurrentHashMap<Integer, ClosableQueue<Completable<T>>> stripes = new ConcurrentHashMap<Integer, ClosableQueue<Completable<T>>>();
	
	private final ClosableQueue<Completable<T>> asyncQueue = new ClosableQueue<Completable<T>>(-1);
	private final ConcurrentLinkedQueue<ClosableQueue<Completable<T>>> queueQueue = new ConcurrentLinkedQueue<ClosableQueue<Completable<T>>>();
	
	private final AtomicBoolean masterQueued = new AtomicBoolean(false);
	private final StripedMergingQueue<T> master;
	
	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();
	
	public StripedQueueImpl() {
		this(null);
	}
	
	public StripedQueueImpl(StripedMergingQueue<T> master) {
		this.master = master;
	}

	public boolean offer(T e) {
		try {
			while (true) {
				ClosableQueue<Completable<T>> queue = getQueue(e.getStripeId());
				readLock.lock();
				try {
					if (queue.offer(new CompletableImpl(e, queue))) {
						return true;
					}
				} finally {
					readLock.unlock();
				}
			}
		} finally {
			notifyQueue();
		}
	}
	
	public T peek() {
		if (master != null) {
			throw new UnsupportedOperationException("Peek operations are not permitted when a master queue is set");
		}
		return peekRaw();
	}
	
	public T peekRaw() {
		Completable<T> o = asyncQueue.peek();
		if (o != null) {
			return o.getStriped();
		}

		for (ClosableQueue<Completable<T>> l : queueQueue) {
			o = l.peek();
			if (o != null) {
				return o.getStriped();
			}
		}
		
		return null;
	}
	
	public Completable<T> poll() {
		if (master != null) {
			throw new UnsupportedOperationException("Poll operations are not permitted when a master queue is set");
		}
		return pollRaw();
	}
	
	protected Completable<T> pollRaw() {
		Completable<T> o = asyncQueue.poll();
		if (o != null) {
			return o;
		}
		ClosableQueue<Completable<T>> l;
		
		while ((l = queueQueue.poll()) != null) {
			do {
				o = l.poll();
				if (o != null) {
					return o;
				}
			} while (!destroyQueue(l));
		}

		return null;
	}
	
	public Completable<T> take() throws InterruptedException {
		if (master != null) {
			throw new UnsupportedOperationException("Take operations are not permitted when a master queue is set");
		}
		return takeRaw();
	}
	
	protected Completable<T> takeRaw() throws InterruptedException {
		Completable<T> o = poll();
		
		if (o != null) {
			return o;
		}
		while (true) {
			synchronized (this) {
				o = poll();
				if (o != null) {
					return o;
				}
				wait();
			}
		}
	}
	
	public boolean isEmpty() {
		if (master != null) {
			throw new UnsupportedOperationException("IsEmpty operations are not permitted when a master queue is set");
		}
		return isEmptyRaw();
	}
	
	protected boolean isEmptyRaw() {
		if (!asyncQueue.isEmpty()) {
			return false;
		}
		for (ClosableQueue<Completable<T>> stripeQueue : stripes.values()) {
			if (!stripeQueue.isEmpty()) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isAvailable() {
		if (asyncQueue.peek() != null) {
			return true;
		}
		for (ClosableQueue<Completable<T>> stripeQueue : queueQueue) {
			if (stripeQueue.peek() != null) {
				return true;
			}
		}
		return false;
	}
	
	private ClosableQueue<Completable<T>> getQueue(int stripeId) {
		
		if (stripeId == -1) {
			return asyncQueue;
		}
		
		ClosableQueue<Completable<T>> queue = stripes.get(stripeId);
		
		if (queue == null) {
			queue = createQueue(stripeId);
		}
		
		return queue;
		
	}
	
	private ClosableQueue<Completable<T>> createQueue(int stripeId) {

		ClosableQueue<Completable<T>> queue = new ClosableQueue<Completable<T>>(stripeId);
		ClosableQueue<Completable<T>> old = stripes.putIfAbsent(stripeId, queue);
		if (old == null) {
			queueQueue.offer(queue);
			return queue;
		}
		return old;
	}
	
	private boolean destroyQueue(ClosableQueue<Completable<T>> queue) {
		if (queue.close()) {
			if (!stripes.remove(queue.getId(), queue)) {
				throw new IllegalStateException("Unable to remove queue from cache");
			}
			return true;
		}
		return false;
	}
	
	private void notifyQueue() {
		if (master != null) {
			notifyMaster();
		} else {
			synchronized(this) {
				notify();
			}
		}
	}
	
	protected void notifyMaster() {
		if (masterQueued.compareAndSet(false, true)) {
			master.notify(this);
		} else {
			master.notifyOne();
		}
	}
	
	public boolean notifyUnqueued() {
		writeLock.lock();
		try {
			if (isAvailable()) {
				return false;
			}
			if (!masterQueued.compareAndSet(true, false)) {
				throw new IllegalStateException("Master queue unqueued this queue before it was queued");
			}
			return true;
		} finally {
			writeLock.unlock();
		}
	}
	
	private class CompletableImpl implements Completable<T> {
		
		private final ClosableQueue<Completable<T>> queue;
		private final int stripeId;
		private final T striped;
		private final AtomicBoolean done = new AtomicBoolean(false);
		
		public CompletableImpl(T striped, ClosableQueue<Completable<T>> queue) {
			this.striped = striped;
			this.stripeId = striped.getStripeId();
			this.queue = queue;
		}

		public int getStripeId() {
			return stripeId;
		}

		public void done() {
			if (queue == asyncQueue) {
				return;
			} else {
				if (done.compareAndSet(false, true)) {
					queueQueue.add(queue);
					StripedQueueImpl.this.notifyQueue();
				} else {
					throw new IllegalStateException("done() called more than once for Completable");
				}
			}
		}

		public T getStriped() {
			return striped;
		}
		
	}
	
}
