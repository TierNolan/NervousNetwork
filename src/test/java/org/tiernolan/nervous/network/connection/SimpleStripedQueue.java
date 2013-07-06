package org.tiernolan.nervous.network.connection;

import java.util.LinkedList;

import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.PacketWrapper;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SimpleStripedQueue implements StripedQueue<PacketWrapper<SimpleConnection>> {
	
	private LinkedList<PacketWrapper<SimpleConnection>> queue = new LinkedList<PacketWrapper<SimpleConnection>>();

	public boolean offer(PacketWrapper<SimpleConnection> w) {
		return queue.offer(w);
	}

	public PacketWrapper<SimpleConnection> peek() {
		return queue.peek();
	}

	public Completable<PacketWrapper<SimpleConnection>> poll() {
		PacketWrapper<SimpleConnection> w = queue.poll();
		if (w == null) {
			return null;
		}
		return new SimpleCompletableStriped(w);
	}

	public Completable<PacketWrapper<SimpleConnection>> take() throws InterruptedException {
		PacketWrapper<SimpleConnection> w = queue.poll();
		if (w == null) {
			throw new IllegalStateException("Attempt made to take packet when queue was empty");
		}
		return new SimpleCompletableStriped(w);		
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	private class SimpleCompletableStriped implements Completable<PacketWrapper<SimpleConnection>> {
		
		private final PacketWrapper<SimpleConnection> wrapper;
		
		public SimpleCompletableStriped(PacketWrapper<SimpleConnection> wrapper) {
			this.wrapper = wrapper;
		}

		public int getStripeId() {
			return wrapper.getStripeId();
		}

		public void done() {
		}

		public PacketWrapper<SimpleConnection> getStriped() {
			return wrapper;
		}
		
	}
	
}