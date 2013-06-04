package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.tiernolan.nervous.network.NetworkManagerImpl;
import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.protocol.Decoder;
import org.tiernolan.nervous.network.api.protocol.Encoder;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SerdesImpl implements Serdes {
	
	private final static Packet shutdownPacket = new Packet() {
		public Protocol getProtocol() {
			return null;
		}
		public int getStripeId() {
			return 0;
		}
	};
	
	private final NetworkManager manager;
	private final Protocol protocol;
	private final Network network;
	private final StripedQueue<Packet> handlerQueue;
	private final ConcurrentLinkedQueue<Packet> writeQueue = new ConcurrentLinkedQueue<Packet>();
	
	private boolean shutdown = false;
	
	private Reference<ByteBuffer> headerRef;
	private ByteBuffer header;
	private Reference<ByteBuffer> bodyRef;
	private ByteBuffer body;
	private Reference<ByteBuffer> writeRef;
	private ByteBuffer write;
	
	private boolean readingHeader;
	private boolean seeking;

	public SerdesImpl(NetworkManager manager, Network network, StripedQueue<Packet> handlerQueue) {
		this.manager = manager;
		this.protocol = manager.getProtocol();
		this.readingHeader = false;
		this.seeking = true;
		this.handlerQueue = handlerQueue;
		this.network = network;
	}

	public Protocol getProtocol() {
		return protocol;
	}
	
	public NetworkManager getNetworkManager() {
		return manager;
	}
	
	public int read(ReadableByteChannel channel) throws IOException {
		int read = 0;
		int r = 0;
		while (true) {
			if (seeking) {
				if (header == null) {
					headerRef = ((NetworkManagerImpl) manager).getByteBufferPool().get(protocol.getPacketHeaderSize());
					header = headerRef.get();
				}
				read += Math.max(0, r = channel.read(header));
				if (header.hasRemaining()) {
					return (r == -1 && read == 0) ? -1 : read;
				}
				int limit = header.limit();
				header.flip();
				try {
					if (!protocol.seekToHeader(header)) {
						continue;
					}
				} finally {
					header.compact();
					header.limit(limit);
				}
				seeking = false;
				readingHeader = true;
			} else if (readingHeader) {
				read += Math.max(0, r = channel.read(header));
				if (header.hasRemaining()) {
					return (r == -1 && read == 0) ? -1 : read;
				}
				readingHeader = false;
				header.flip();
			} else {
				if (body == null) {
					int size = protocol.getPacketBodySize(header);
					bodyRef = ((NetworkManagerImpl) manager).getByteBufferPool().get(size);
					body = bodyRef.get();
				}
				read += Math.max(0, r = channel.read(body));
				if (!body.hasRemaining()) {
					body.flip();
					Decoder<?> decoder = protocol.getPacketDecoder(header);
					if (decoder == null) {
						throw new IOException("No decoder found for packet header");
					}
					Packet p = decoder.decode(header, body);
					if (p == null) {
						throw new IOException("Decoding failed for packet");
					}
					handlerQueue.offer(p);
					((NetworkManagerImpl) manager).getByteBufferPool().put(bodyRef);
					((NetworkManagerImpl) manager).getByteBufferPool().put(headerRef);
					body = null;
					bodyRef = null;
					header = null;
					headerRef = null;
					seeking = true;
				} else {
					return (r == -1 && read == 0) ? -1 : read;
				}
			}
		}
	}

	public int write(WritableByteChannel channel) throws IOException {
		
		int i = 0;
		
		boolean blocked = false;
		while (!blocked) {
			if (write == null) {
				Packet p;
				if (shutdown || (p = writeQueue.poll()) == null) {
					network.clearWriteRequest();
					p = writeQueue.poll();
					if (shutdown || p == null) {
						return i;
					}
				}
				if (p == shutdownPacket) {
					shutdown = true;
					if (channel instanceof SocketChannel) {
						network.close();
					}
					continue;
				}
				@SuppressWarnings("unchecked")
				Encoder<Packet> e = (Encoder<Packet>) protocol.getPacketEncoder(p);
				if (e == null) {
					throw new IOException("No encoder found for packet");
				}
				int size = protocol.getPacketHeaderSize() + e.getPacketBodySize(p);
				writeRef = ((NetworkManagerImpl) manager).getByteBufferPool().get(size);
				write = writeRef.get();
				e.encode(p, write);
				write.flip();
			}
			int written = channel.write(write);
			
			if (!write.hasRemaining()) {
				((NetworkManagerImpl) manager).getByteBufferPool().put(writeRef);
				write = null;
				writeRef = null;
			}
			
			i += written;
			blocked = written == 0;
		}
		
		return i;
	}
	
	public void writePacket(Packet p) {
		writeQueue.add(p);
		network.setWriteRequest();
	}

	public void shutdown() {
		writePacket(shutdownPacket);
	}

}
