package org.tiernolan.nervous.network.connection;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.tiernolan.nervous.network.NetworkManagerImpl;
import org.tiernolan.nervous.network.api.NetworkComponent;
import org.tiernolan.nervous.network.api.NetworkManager;
import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.protocol.Decoder;
import org.tiernolan.nervous.network.api.protocol.Encoder;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;
import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public class SerDes implements ProtocolComponent, NetworkComponent {
	
	private final NetworkManager manager;
	private final Protocol protocol;
	private final Connection connection;
	
	private Reference<ByteBuffer> headerRef;
	private ByteBuffer header;
	private Reference<ByteBuffer> bodyRef;
	private ByteBuffer body;
	private boolean readingHeader;

	public SerDes(NetworkManager manager, Connection connection) {
		this.manager = manager;
		this.protocol = connection.getProtocol();
		this.connection = connection;
		this.readingHeader = true;
	}

	public Protocol getProtocol() {
		return protocol;
	}
	
	public NetworkManager getNetworkManager() {
		return manager;
	}
	
	public int read(ReadableByteChannel channel) throws IOException {
		int read = 0;
		while (true) {
			if (readingHeader) {
				if (header == null) {
					headerRef = ((NetworkManagerImpl) manager).getByteBufferPool().get(protocol.getPacketHeaderSize());
					header = headerRef.get();
				}
				read += channel.read(header);
				if (header.hasRemaining()) {
					return read;
				}
				readingHeader = false;			
			}
			if (body == null) {
				int size = protocol.getPacketBodySize(header);
				bodyRef = ((NetworkManagerImpl) manager).getByteBufferPool().get(size);
				body = bodyRef.get();
			}
			read += channel.read(body);
			if (!body.hasRemaining()) {
				Decoder<?> decoder = protocol.getPacketDecoder(header);
				if (decoder == null) {
					manager.getLogger().info("No decoder found for connection " + connection);
					connection.close();
					return -1;
				}
				Packet p = decoder.decode(connection, header, body);
				if (p == null) {
					manager.getLogger().info("Unable to decode packet for connection " + connection);
					connection.close();
					return -1;
				}
				//manager.handle(connection, p);
				body = null;
				bodyRef = null;
				header = null;
				headerRef = null;
			} else {
				return read;
			}
		}
	}

	public void write(Packet packet) {
		Encoder<?> encoder = protocol.getPacketEncoder(packet);
		//encoder.encode(connection, packet, header, body)
	}

}