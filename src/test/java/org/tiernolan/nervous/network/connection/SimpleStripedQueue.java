package org.tiernolan.nervous.network.connection;

import java.util.LinkedList;

import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SimpleStripedQueue implements StripedQueue<Packet<SimpleConnection>> {
	
	private LinkedList<Packet<SimpleConnection>> queue = new LinkedList<Packet<SimpleConnection>>();

	public boolean offer(Packet<SimpleConnection> p) {
		return queue.offer(p);
	}

	public Packet<SimpleConnection> peek() {
		return queue.peek();
	}

	public Completable<Packet<SimpleConnection>> poll() {
		Packet<SimpleConnection> p = queue.poll();
		if (p == null) {
			return null;
		}
		return new SimpleCompletableStriped(p);
	}

	public Completable<Packet<SimpleConnection>> take() throws InterruptedException {
		Packet<SimpleConnection> p = queue.poll();
		if (p == null) {
			throw new IllegalStateException("Attempt made to take packet when queue was empty");
		}
		return new SimpleCompletableStriped(p);		
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	private class SimpleCompletableStriped implements Completable<Packet<SimpleConnection>> {
		
		private final Packet<SimpleConnection> packet;
		
		public SimpleCompletableStriped(Packet<SimpleConnection> packet) {
			this.packet = packet;
		}

		public int getStripeId() {
			return packet.getStripeId();
		}

		public void done() {
		}

		public Packet<SimpleConnection> getStriped() {
			return packet;
		}
		
	}
	
}