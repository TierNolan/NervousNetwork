package org.tiernolan.nervous.network.connection;

import java.util.LinkedList;

import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.queue.Completable;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SimpleStripedQueue implements StripedQueue<Packet> {
	
	private LinkedList<Packet> queue = new LinkedList<Packet>();

	public boolean offer(Packet p) {
		return queue.offer(p);
	}

	public Packet peek() {
		return queue.peek();
	}

	public Completable<Packet> poll() {
		Packet p = queue.poll();
		if (p == null) {
			return null;
		}
		return new SimpleCompletableStriped(p);
	}

	public Completable<Packet> take() throws InterruptedException {
		Packet p = queue.poll();
		if (p == null) {
			throw new IllegalStateException("Attempt made to take packet when queue was empty");
		}
		return new SimpleCompletableStriped(p);		}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	private class SimpleCompletableStriped implements Completable<Packet> {
		
		private final Packet packet;
		
		public SimpleCompletableStriped(Packet packet) {
			this.packet = packet;
		}

		public int getStripeId() {
			return packet.getStripeId();
		}

		public void done() {
		}

		public Packet getStriped() {
			return packet;
		}
		
	}
	
}