package org.tiernolan.nervous.network.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Handler;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;

public class StripedExecutor<C extends Connection<C>> {

	private static AtomicInteger idCounter = new AtomicInteger(1);
	
	private final Protocol<C> protocol;
	private final NetworkManager<C> manager;
	private final StripedQueue<PacketWrapper<C>> queue;
	private final AtomicReferenceArray<PoolThread> threads;
	private final AtomicBoolean running = new AtomicBoolean(true);
	
	public StripedExecutor(NetworkManager<C> manager, StripedQueue<PacketWrapper<C>> queue) {
		this(manager, queue, Runtime.getRuntime().availableProcessors());
	}

	public StripedExecutor(NetworkManager<C> manager, StripedQueue<PacketWrapper<C>> queue, int poolSize) {
		this.queue = queue;
		this.manager = manager;
		this.protocol = manager.getProtocol();
		this.threads = new AtomicReferenceArray<PoolThread>(poolSize);
		for (int i = 0; i < poolSize; i++) {
			addThread(null, i);
		}
	}
	
	private void addThread(PoolThread old, int index) {
		synchronized (running) {
			if (running.get()) {
				PoolThread t = new PoolThread(index);
				if (threads.compareAndSet(index, old, t)) {
					t.start();
				} else {
					manager.getLogger().info("Illegal state, unable to add PoolThread");
				}
			}
		}
	}
	
	public void shutdown() {
		synchronized (running) {
			if (running.compareAndSet(true, false)) {
				for (int i = 0; i < threads.length(); i++) {
					threads.get(i).interrupt();
				}
			}
		}
	}
	
	public List<Thread> getThreads() {
		List<Thread> list = new ArrayList<Thread>(threads.length());
		for (int i = 0; i < threads.length(); i++) {
			list.add(threads.get(i));
		}
		return list;
	}
	
	private class PoolThread extends Thread {
				
		private final int index;
		
		public PoolThread(int index) {
			this.setName("StripedExecutor Thread " + idCounter.getAndIncrement());
			this.index = index;
		}
		
		public void run() {
			try {
				while (running.get()) {
					Completable<PacketWrapper<C>> c;
					try {
						c = queue.take();
					} catch (InterruptedException e) {
						continue;
					}
					if (c == null) {
						manager.getLogger().info("Retrieved null completable from queue");
						continue;
					}
					try {
						PacketWrapper<C> w = c.getStriped();
						if (w == null) {
							manager.getLogger().info("Retrieved null packet wrapper from queue");
							continue;
						}
						Packet<C> p = w.getPacket();
						Connection<C> conn = w.getConnection();
						Handler<Packet<C>, C> handler;
						try {
							handler = protocol.getPacketHandler(p);
						} catch (Throwable t) {
							manager.getLogger().info("Protocol threw " + t + " when trying to determine handler for " + p);
							try {
								conn.shutdown();
							} catch (Throwable t2) {
								manager.getLogger().info("Connection threw " + t2 + " when trying to shutdown after " + t + " was thrown");
							}
							continue;
						}
						try {
							handler.handle(conn, p);
						} catch (Throwable t) {
							manager.getLogger().info("Handler threw " + t + " when trying to handler " + p);
							try {
								conn.shutdown();
							} catch (Throwable t2) {
								manager.getLogger().info("Connection threw " + t2 + " when trying to shutdown after " + t + " was thrown");
							}
							continue;
						}
					} finally {
						c.done();
					}
				}
			} catch (Throwable t) {
				manager.getLogger().info("Pool thread threw " + t + ", caught by outer catch");
			}
			addThread(this, index);
		}
		
	}
	
}
