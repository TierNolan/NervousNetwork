package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
	private final AtomicReference<HandlerState> inProgress = new AtomicReference<HandlerState>(HandlerState.IDLE);
	
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
					if (!inProgress.compareAndSet(HandlerState.RUNNING, HandlerState.IDLE)) {
						throw new IllegalStateException("Channel Handler was not in RUNNING state");
					}
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
					if (!inProgress.compareAndSet(HandlerState.RUNNING, HandlerState.IDLE)) {
						throw new IllegalStateException("Channel Handler was not in RUNNING state");
					}
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
			if (inProgress.compareAndSet(HandlerState.IDLE, HandlerState.WRITE_PENDING)) {
				queueForSync();
			}
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
		if (inProgress.compareAndSet(HandlerState.WRITE_PENDING, HandlerState.IDLE)) {
			restoreOps();
		}
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
		setInProgress();
		return readRunnable;
	}
	
	public Runnable getWriteRunnable() {
		setInProgress();
		return writeRunnable;
	}
	
	private void setInProgress() {
		if (!inProgress.compareAndSet(HandlerState.IDLE, HandlerState.RUNNING) && !inProgress.compareAndSet(HandlerState.WRITE_PENDING, HandlerState.RUNNING)) {
			throw new IllegalStateException("Channel Handler was not in IDLE or WRITE_PENDING state");
		}
		key.interestOps(0);
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
	
	private static enum HandlerState {
		IDLE, WRITE_PENDING, RUNNING;
	}

}
