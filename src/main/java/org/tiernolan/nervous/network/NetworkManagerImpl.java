package org.tiernolan.nervous.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.bufferpool.ByteBufferPool;

public class NetworkManagerImpl implements NetworkManager {
	
	private final Protocol protocol;
	private final ByteBufferPool byteBufferPool;
	private final Logger logger;
	
	private final ConcurrentHashMap<Long, Connection> connections = new  ConcurrentHashMap<Long, Connection>();
	
	public NetworkManagerImpl(Protocol protocol) {
		this.protocol = protocol;
		this.byteBufferPool = new ByteBufferPool(protocol.getMaxPacketSize());
		this.logger = Logger.getLogger(getClass().getName());
	}
	
	public ByteBufferPool getByteBufferPool() {
		return byteBufferPool;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public Logger getLogger() {
		return logger;
	}

}
