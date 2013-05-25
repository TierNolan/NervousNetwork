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

public class AcceptThread extends Thread {

	private final ServerSocketChannel serverChannel;
	private final Selector selector;
	
	public AcceptThread(int port) throws IOException {
		this(new InetSocketAddress(port));
	}

	public AcceptThread(InetSocketAddress addr) throws IOException {
		try {
			this.selector = SelectorProvider.provider().openSelector();
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.bind(addr);
			this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			cleanup();
			throw new IOException("Unable to start accept thread", e);
		}
		this.start();
	}
	
	public void run() {
		try {
			while (!isInterrupted()) {
				int k;
				try {
					k = selector.select();
				} catch (IOException e1) {
					// TODO - Logging
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
						e.printStackTrace();
						continue;
					}
					// add to pool
				}
			}
			
		} finally {
			cleanup();
		}
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
