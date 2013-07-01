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
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Decoder;
import org.tiernolan.nervous.network.api.protocol.Encoder;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.queue.StripedQueue;

public class SerdesImpl<C extends Connection<C>> implements Serdes<C> {
	
	private final Packet<C> shutdownPacket = new Packet<C>() {
		public Protocol<C> getProtocol() {
			return null;
		}
		public int getStripeId() {
			return 0;
		}
	};
	
	private final NetworkManager<C> manager;
	private final Protocol<C> protocol;
	private final ChannelControl channelControl;
	private final StripedQueue<Packet<C>> handlerQueue;
	private final ConcurrentLinkedQueue<Packet<C>> writeQueue = new ConcurrentLinkedQueue<Packet<C>>();
	
	private boolean shutdown = false;
	
	private Reference<ByteBuffer> headerRef;
	private ByteBuffer header;
	private Reference<ByteBuffer> bodyRef;
	private ByteBuffer body;
	private Reference<ByteBuffer> writeRef;
	private ByteBuffer write;
	
	private boolean readingHeader;
	private boolean seeking;

	public SerdesImpl(NetworkManager<C> manager, ChannelControl channelControl, StripedQueue<Packet<C>> handlerQueue) {
		this.manager = manager;
		this.protocol = manager.getProtocol();
		this.readingHeader = false;
		this.seeking = true;
		this.handlerQueue = handlerQueue;
		this.channelControl = channelControl;
	}

	public Protocol<C> getProtocol() {
		return protocol;
	}
	
	public NetworkManager<C> getNetworkManager() {
		return manager;
	}
	
	public int read(ReadableByteChannel channel) throws IOException {
		int read = 0;
		int r = 0;
		while (true) {
			if (seeking) {
				if (header == null) {
					headerRef = ((NetworkManagerImpl<C>) manager).getByteBufferPool().get(protocol.getPacketHeaderSize());
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
					bodyRef = ((NetworkManagerImpl<C>) manager).getByteBufferPool().get(size);
					body = bodyRef.get();
				}
				read += Math.max(0, r = channel.read(body));
				if (!body.hasRemaining()) {
					body.flip();
					Decoder<Packet<C>, C> decoder = protocol.getPacketDecoder(header);
					if (decoder == null) {
						throw new IOException("No decoder found for packet header");
					}
					Packet<C> p = decoder.decode(header, body);
					if (p == null) {
						throw new IOException("Decoding failed for packet");
					}
					handlerQueue.offer(p);
					((NetworkManagerImpl<C>) manager).getByteBufferPool().put(bodyRef);
					((NetworkManagerImpl<C>) manager).getByteBufferPool().put(headerRef);
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
				Packet<C> p;
				if (shutdown || (p = writeQueue.poll()) == null) {
					channelControl.clearWriteRequest();
					p = writeQueue.poll();
					if (shutdown || p == null) {
						return i;
					}
				}
				if (p == shutdownPacket) {
					shutdown = true;
					if (channel instanceof SocketChannel) {
						channelControl.close();
					}
					continue;
				}
				Encoder<Packet<C>, C> e = protocol.getPacketEncoder(p);
				if (e == null) {
					throw new IOException("No encoder found for packet");
				}
				int size = protocol.getPacketHeaderSize() + e.getPacketBodySize(p);
				writeRef = ((NetworkManagerImpl<C>) manager).getByteBufferPool().get(size);
				write = writeRef.get();
				e.encode(p, write);
				write.flip();
			}
			int written = channel.write(write);
			
			if (!write.hasRemaining()) {
				((NetworkManagerImpl<C>) manager).getByteBufferPool().put(writeRef);
				write = null;
				writeRef = null;
			}
			
			i += written;
			blocked = written == 0;
		}
		
		return i;
	}
	
	public void writePacket(Packet<C> p) {
		writeQueue.add(p);
		channelControl.setWriteRequest();
	}

	public void shutdown() {
		writePacket(shutdownPacket);
	}

}
