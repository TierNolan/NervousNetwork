package org.tiernolan.nervous.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.bufferpool.ByteBufferPool;
import org.tiernolan.nervous.network.connection.ChannelHandler;
import org.tiernolan.nervous.network.connection.SelectorHandler;
import org.tiernolan.nervous.network.queue.StripedMergingQueue;
import org.tiernolan.nervous.network.queue.StripedQueue;
import org.tiernolan.nervous.network.queue.StripedQueueImpl;

public class NetworkManagerImpl<C extends Connection<C>> implements NetworkManager<C> {
	
	private final Protocol<C> protocol;
	private final ByteBufferPool byteBufferPool;
	private final Logger logger;
	private final ExecutorService pool;
	private final SelectorHandler<C>[] selectorHandlers;
	private final AtomicInteger selectorCounter = new AtomicInteger(0);
	private final StripedMergingQueue<Packet<C>> masterQueue;
	private final ConcurrentHashMap<ChannelHandler<C>, Boolean> channels = new ConcurrentHashMap<ChannelHandler<C>, Boolean>();
	private final ConcurrentLinkedQueue<AcceptThread<C>> acceptThreads = new ConcurrentLinkedQueue<AcceptThread<C>>();
	private boolean running = true;
	private final Object configSync = new Object();
	
	public NetworkManagerImpl(Protocol<C> protocol) {
		this(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, protocol);
	}
	
	public NetworkManagerImpl(int selectors, int poolSize, Protocol<C> protocol) {
		this.protocol = protocol;
		this.masterQueue = new StripedMergingQueue<Packet<C>>();
		this.byteBufferPool = new ByteBufferPool(protocol.getMaxPacketSize());
		this.logger = Logger.getLogger(getClass().getName());
		this.pool = Executors.newFixedThreadPool(poolSize);
		this.selectorHandlers = getSelectorHandlerArray(selectors);
		for (int i = 0; i < selectorHandlers.length; i++) {
			try {
				this.selectorHandlers[i] = new SelectorHandler<C>(this);
			} catch (IOException e) {
				getLogger().info("Unable to setup selectors " + e);
				for (int j = 0; j < i; j++) {
					this.selectorHandlers[j].closeSelector();
				}
			}
		}
		for (int i = 0; i < selectorHandlers.length; i++) {
			selectorHandlers[i].start();
		}
	}
	
	@SuppressWarnings("unchecked")
	private SelectorHandler<C>[] getSelectorHandlerArray(int size) {
		return new SelectorHandler[size];
	}
	
	public ByteBufferPool getByteBufferPool() {
		return byteBufferPool;
	}

	public Protocol<C> getProtocol() {
		return protocol;
	}
	
	public boolean addChannel(SocketChannel channel) {
		SelectorHandler<C> selectorHandler = selectorHandlers[selectorCounter.getAndIncrement() % selectorHandlers.length];
		StripedQueue<Packet<C>> channelQueue = new StripedQueueImpl<Packet<C>>(masterQueue);
		ChannelHandler<C> channelHandler = selectorHandler.addChannel(channel, channelQueue);
		return channelHandler != null;
	}
	
	public void register(ChannelHandler<C> handler) {
		if (channels.putIfAbsent(handler, Boolean.TRUE) != null) {
			throw new IllegalStateException("Channel added to manager more than once");
		}
	}
	
	public void deregister(ChannelHandler<C> handler) {
		if (!channels.remove(handler, Boolean.TRUE)) {
			throw new IllegalStateException("Attempt to remove unknown channel from manager");
		}
	}

	public Logger getLogger() {
		return logger;
	}

	public ExecutorService getExecutorService() {
		return pool;
	}

	public void listen(int port) throws IOException {
		synchronized (configSync) {
			if (!running) {
				return;
			}
			AcceptThread<C> t = new AcceptThread<C>(this, port);
			acceptThreads.add(t);
			t.start();
		}
	}

	public void listen(InetSocketAddress addr) throws IOException {
		synchronized (configSync) {
			if (!running) {
				return;
			}
			AcceptThread<C> t = new AcceptThread<C>(this, addr);
			acceptThreads.add(t);
			t.start();
		}
	}
	
	public void shutdown() {
		shutdown(0);
	}
	
	public void shutdown(long timeout) {
		synchronized (configSync) {
			running = false;
		}
		for (AcceptThread<C> a : acceptThreads) {
			a.shutdown(timeout);
		}
		for (SelectorHandler<C> h : selectorHandlers) {
			h.shutdown(timeout);
		}
		List<Thread> threads = new ArrayList<Thread>(acceptThreads.size() + selectorHandlers.length);
		threads.addAll(acceptThreads);
		for (Thread s : selectorHandlers) {
			threads.add(s);
		}
		join(threads, timeout);
	}

	private void join(List<Thread> threads, long timeout) {
		boolean interrupted = false;
		try {
			long start = System.currentTimeMillis();
			for (Thread t : threads) {
				boolean joined = false;
				while (!joined) {
					try {
						if (timeout > 0) {
							long delay = System.currentTimeMillis() - start;
							if (delay > 0) {
								t.join(delay);
							}
						} else {
							t.join();
						}
						joined = true;
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
