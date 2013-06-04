package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class ChannelHandler implements Network {
	
	private final static AtomicInteger hashCount = new AtomicInteger(0);
	
	private final NetworkManager manager;
	private final Serdes serdes;
	private final SelectorHandler selectorHandler;
	private final SocketChannel channel;
	private final int hash;
	
	private final SelectionKey key;
	private final AtomicBoolean writePending = new AtomicBoolean(false);
	private final AtomicBoolean closePending = new AtomicBoolean(false);
	private final AtomicBoolean syncPending = new AtomicBoolean(false);
	private boolean write = false;
	
	private final Runnable readRunnable;
	private final Runnable writeRunnable;
	
	public ChannelHandler(final NetworkManager manager, final SocketChannel channel, final SelectorHandler selectorHandler, StripedQueue<Packet> queue) throws IOException {
		readRunnable = new Runnable() {
			public void run() {
				try {
					serdes.read(channel);
				} catch (IOException e) {
					close();
				} finally {
					restoreOps();
				}
			}
		};
		writeRunnable = new Runnable() {
			public void run() {
				try {
					serdes.write(channel);
				} catch (IOException e) {
					close();
				} finally {
					restoreOps();
				}
			}
		};
		this.serdes = new SerdesImpl(manager, this, queue);
		this.hash = hashCount.incrementAndGet();
		this.manager = manager;
		this.channel = channel;
		this.selectorHandler = selectorHandler;
		channel.configureBlocking(false);
		try {
			this.key = selectorHandler.register(channel, this);
			if (key == null) {
				throw new IOException("SelectorHandler is not running");
			}
		} catch (IOException e) {
			e.printStackTrace();
			close();
			throw e;
		}
	}
	
	public void clearWriteRequest() {
		write = false;
	}

	public void setWriteRequest() {
		if (writePending.compareAndSet(false, true)) {
			queueForSync();
		}
	}
	
	public void shutdown(long timeout) {
		serdes.shutdown();
		if (timeout > 0) {
			selectorHandler.getTimer().schedule(new TimerTask() {
				@Override
				public void run() {
					asyncClose();
				}
			}, timeout);
		}
	}
	
	public void asyncClose() {
		if (closePending.compareAndSet(false, true)) {
			queueForSync();
		}
	}
	
	public void close() {
		try {
			if (key != null) {
				key.cancel();
			}
			try {
				channel.close();
			} catch (IOException e) {
			}
		} finally {
			selectorHandler.notifyClosed(this);
		}

	}
	
	public void queueForSync() {
		if (syncPending.compareAndSet(false, true)) {
			selectorHandler.queueForSync(this);
		}
	}
	
	public void sync() {
		if (!syncPending.compareAndSet(true, false)) {
			throw new IllegalStateException("Sync flag was false when syncing");
		}
		restoreOps();
		if (closePending.get()) {
			close();
		}
	}
	
	private void restoreOps() {
		if (writePending.compareAndSet(true, false)) {
			write = true;
		}
		if (write) {
			selectorHandler.setReadWrite(key);
		} else {
			selectorHandler.setReadOnly(key);
		}
	}

	public Runnable getReadRunnable() {
		return readRunnable;
	}
	
	public Runnable getWriteRunnable() {
		return writeRunnable;
	}
	
	protected Serdes getSerdes() {
		return serdes;
	}
	
	@Override
	public boolean equals(Object o) {
		return o == this;
	}
	
	@Override
	public int hashCode() {
		return hash;
	}

}
