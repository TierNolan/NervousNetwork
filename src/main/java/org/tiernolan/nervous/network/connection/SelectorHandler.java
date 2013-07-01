package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.tiernolan.nervous.network.NetworkManagerImpl;
import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SelectorHandler<C extends Connection<C>> extends Thread {

	private final NetworkManager<C> manager;

	private final Selector selector;

	private final ReentrantReadWriteLock guard = new ReentrantReadWriteLock();
	private final Lock keyLock = guard.readLock();
	private final Lock selectorLock = guard.writeLock();
	private volatile boolean running = true;
	
	private final Timer timer = new Timer();
	
	private final ConcurrentLinkedQueue<ChannelHandler<C>> channelHandlerSyncQueue = new ConcurrentLinkedQueue<ChannelHandler<C>>();
	private final ConcurrentHashMap<ChannelHandler<C>, Boolean> channels = new ConcurrentHashMap<ChannelHandler<C>, Boolean>();

	public SelectorHandler(NetworkManager<C> manager) throws IOException {
		this.setName("SelectorHandler {" + manager + "}");
		this.selector = SelectorProvider.provider().openSelector();
		this.manager = manager;
	}

	@Override
	public void run() {

		try {
			while (running || !channels.isEmpty()) {

				if (interrupted()) {
					shutdown(0);
					continue;
				}

				touchGuard();
				int n;
				try {
					n = selector.select();
				} catch (IOException e) {
					manager.getLogger().info("IOException thrown, " + e.getMessage());
					break;
				}
				ChannelHandler<C> h;
				while ((h = channelHandlerSyncQueue.poll()) != null) {
					h.sync();
				}
				if (n > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();

					Iterator<SelectionKey> i = keys.iterator();
					while (i.hasNext()) {
						SelectionKey key = i.next();
						i.remove();
						if (!key.isValid()) {
							continue;
						}
						if (key.isReadable()) {
							@SuppressWarnings("unchecked")
							ChannelHandler<C> channelHandler = (ChannelHandler<C>) key.attachment();
							manager.getExecutorService().submit(channelHandler.getReadRunnable());
						} else if (key.isWritable()) {
							@SuppressWarnings("unchecked")
							ChannelHandler<C> channelHandler = (ChannelHandler<C>) key.attachment();
							manager.getExecutorService().submit(channelHandler.getWriteRunnable());
						}
					}
				}
			}
		} finally {
			closeSelector();
			timer.cancel();
		}
	}

	public boolean setReadWrite(SelectionKey key) {
		return setOps(key, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}

	public boolean setReadOnly(SelectionKey key) {
		return setOps(key, SelectionKey.OP_READ);
	}

	public boolean setOps(SelectionKey key, int ops) {
		keyLock.lock();
		try {
			if (!key.isValid()) {
				return false;
			}
			boolean oldWrite = (key.interestOps() & SelectionKey.OP_WRITE) != 0;
			selector.wakeup();
			key.interestOps(ops);
			return oldWrite;
		} finally {
			keyLock.unlock();
		}
	}

	public SelectionKey register(SocketChannel channel, ChannelHandler<C> channelHandler) throws IOException {
		keyLock.lock();
		try {
			if (!running) {
				return null;
			}
			selector.wakeup();
			return channel.register(selector, SelectionKey.OP_READ, channelHandler);
		} finally {
			keyLock.unlock();
		}
	}

	public ChannelHandler<C> addChannel(SocketChannel channel, StripedQueue<Packet<C>> queue) {
		keyLock.lock();
		try {
			if (!running) {
				return null;
			}
			ChannelHandler<C> channelHandler;
			synchronized (channels) {
				try {
					channelHandler = new ChannelHandler<C>(manager, channel, this, queue);
				} catch (IOException e) {
					manager.getLogger().info("Unable to create channel handler, " + e.getMessage());
					return null;
				}
				channels.put(channelHandler, Boolean.TRUE);
				if (manager instanceof NetworkManagerImpl) {
					((NetworkManagerImpl<C>) manager).register(channelHandler);
				}
			}
			return channelHandler;
		} finally {
			keyLock.unlock();
		}
	}

	public void closeSelector() {
		selectorLock.lock();
		try {
			try {
				if (selector != null) {
					selector.close();
				}
			} catch (IOException e) {
				manager.getLogger().info("Exception thrown when closing selector " + e);
			}
		} finally {
			selectorLock.unlock();
		}
	}

	public void shutdown(long timeout) {
		selectorLock.lock();
		try {
			running = false;
			Set<SelectionKey> keys = selector.keys();
			if (keys.size() == 0) {
				selector.wakeup();
			} else {
				for (SelectionKey key : keys) {
					if (key.isValid()) {
						@SuppressWarnings("unchecked")
						ChannelHandler<C> handler = ((ChannelHandler<C>) key.attachment());
						handler.shutdown(timeout);
					}
				}
			}
		} finally {
			selectorLock.unlock();
		}
	}
	
	protected Timer getTimer() {
		return timer;
	}

	public void notifyClosed(ChannelHandler<C> handler) {
		keyLock.lock();
		try {
			if (channels.remove(handler)) {
				if (manager instanceof NetworkManagerImpl) {
					((NetworkManagerImpl<C>) manager).deregister(handler);
				}
				selector.wakeup();
			}
		} finally {
			keyLock.unlock();
		}
	}

	public void queueForSync(ChannelHandler<C> handler) {
		channelHandlerSyncQueue.add(handler);
		selector.wakeup();
	}

	private void touchGuard() {
		try {
			selectorLock.lock();
		} finally {
			selectorLock.unlock();
		}
	}

}
