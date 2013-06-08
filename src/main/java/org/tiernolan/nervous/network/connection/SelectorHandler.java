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

import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SelectorHandler extends Thread {

	private final NetworkManager manager;

	private final Selector selector;

	private final ReentrantReadWriteLock guard = new ReentrantReadWriteLock();
	private final Lock keyLock = guard.readLock();
	private final Lock selectorLock = guard.writeLock();
	private volatile boolean running = true;
	
	private final Timer timer = new Timer();
	
	private final ConcurrentLinkedQueue<ChannelHandler> channelHandlerSyncQueue = new ConcurrentLinkedQueue<ChannelHandler>();
	private final ConcurrentHashMap<ChannelHandler, Boolean> channels = new ConcurrentHashMap<ChannelHandler, Boolean>();

	public SelectorHandler(NetworkManager manager) throws IOException {
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
				ChannelHandler h;
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
							ChannelHandler channelHandler = (ChannelHandler) key.attachment();
							manager.getExecutorService().submit(channelHandler.getReadRunnable());
						} else if (key.isWritable()) {
							ChannelHandler channelHandler = (ChannelHandler) key.attachment();
							manager.getExecutorService().submit(channelHandler.getWriteRunnable());
						}
					}
				}
			}
		} finally {
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

	public SelectionKey register(SocketChannel channel, ChannelHandler channelHandler) throws IOException {
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

	public ChannelHandler addChannel(SocketChannel channel, StripedQueue<Packet> queue) throws IOException {
		keyLock.lock();
		try {
			if (!running) {
				return null;
			}
			ChannelHandler channelHandler;
			try {
				channelHandler = new ChannelHandler(manager, channel, this, queue);
			} catch (IOException e) {
				manager.getLogger().info("Unable to create channel handler, " + e.getMessage());
				return null;
			}
			channels.put(channelHandler, Boolean.TRUE);
			return channelHandler;
		} finally {
			keyLock.unlock();
		}
	}


	public void shutdown(long timeout) {
		selectorLock.lock();
		try {
			running = false;
			Set<SelectionKey> keys = selector.keys();
			for (SelectionKey key : keys) {
				if (key.isValid()) {
					((ChannelHandler) key.attachment()).shutdown(timeout);
				}
			}
		} finally {
			selectorLock.unlock();
		}
	}
	
	protected Timer getTimer() {
		return timer;
	}

	public void notifyClosed(ChannelHandler handler) {
		if (channels.remove(handler)) {
			//manager.remove(handler);
			selector.wakeup();
		}
	}
	
	public void queueForSync(ChannelHandler handler) {
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
