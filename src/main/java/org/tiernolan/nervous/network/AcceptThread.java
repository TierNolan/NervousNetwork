package org.tiernolan.nervous.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptThread extends Thread {

	private final ServerSocketChannel serverChannel;
	
	public AcceptThread(int port) throws IOException {
		this(new InetSocketAddress(port));
	}

	public AcceptThread(InetSocketAddress addr) throws IOException {
		try {
			this.serverChannel = ServerSocketChannel.open();
			this.serverChannel.configureBlocking(true);
			this.serverChannel.bind(addr);
		} catch (IOException e) {
			cleanup();
			throw new IOException("Unable to start accept thread", e);
		}
		this.start();
	}
	
	public void run() {
		try {
			while (!isInterrupted()) {
				SocketChannel channel;
				try {
					channel = serverChannel.accept();
				} catch (IOException e) {
					continue;
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
	}

}
