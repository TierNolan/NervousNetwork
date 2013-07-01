package org.tiernolan.nervous.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;

import org.tiernolan.nervous.network.api.connection.Connection;

public class AcceptThread<C extends Connection<C>> extends Thread {

	private final NetworkManagerImpl<C> manager;
	private final ServerSocketChannel serverChannel;
	private final Selector selector;
	private volatile boolean running = true;
	
	public AcceptThread(NetworkManagerImpl<C> manager, int port) throws IOException {
		this(manager, new InetSocketAddress(port));
	}

	public AcceptThread(NetworkManagerImpl<C> manager, InetSocketAddress addr) throws IOException {
		try {
			this.setName("AcceptThread {" + addr + "}");
			this.manager = manager;
			this.selector = SelectorProvider.provider().openSelector();
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.bind(addr);
			this.serverChannel.configureBlocking(false);
			this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			cleanup();
			throw new IOException("Unable to start accept thread", e);
		}
	}
	
	public void run() {
		try {
			while (running) {
				int k;
				try {
					k = selector.select();
				} catch (IOException e) {
					manager.getLogger().info("Exception thrown by accept thread selector " + e);
					break;
				}
				if (k <= 0) {
					continue;
				}
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> i = keys.iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
					SocketChannel channel;
					try {
						channel = serverChannel.accept();
					} catch (IOException e) {
						manager.getLogger().info("Exception thrown by server socket when accepting " + e);
						continue;
					}
					manager.addChannel(channel);
				}
			}
		} finally {
			cleanup();
		}
	}
	
	public void shutdown(long timeout) {
		running = false;
		selector.wakeup();
	}

	private void cleanup() {
		if (this.serverChannel != null) {
			try {
				this.serverChannel.close();
			} catch (IOException e1) {
			}
		}
		if (this.selector != null) {
			try {
				this.selector.close();
			} catch (IOException ioe) {
			}
		}
	}

}
