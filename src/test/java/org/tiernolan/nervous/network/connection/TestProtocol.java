package org.tiernolan.nervous.network.connection;

import java.nio.ByteBuffer;

import org.tiernolan.nervous.network.api.protocol.Decoder;
import org.tiernolan.nervous.network.api.protocol.Encoder;
import org.tiernolan.nervous.network.api.protocol.Handler;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;

public class TestProtocol implements Protocol {
	
	private final int[] bodySizes = new int[] {4, 8}; 
	private final Decoder<?>[] decoders = new Decoder[] {
			new Decoder<GenericPacket>() {

				public Protocol getProtocol() {
					return TestProtocol.this;
				}

				public GenericPacket decode(ByteBuffer header, ByteBuffer body) {
					final Integer data = body.getInt();
					return new GenericPacket(0) {
						@Override
						public Object getData() {
							return data;
						}
					};
				}
			},
			new Decoder<GenericPacket>() {

				public Protocol getProtocol() {
					return TestProtocol.this;
				}

				public GenericPacket decode(ByteBuffer header, ByteBuffer body) {
					final Long data = body.getLong();
					return new GenericPacket(1) {
						@Override
						public Object getData() {
							return data;
						}
					};
				}
			}
	};
	
	private final Encoder<?>[] encoders = new Encoder[] {
			new Encoder<GenericPacket>() {

				public Protocol getProtocol() {
					return TestProtocol.this;
				}

				public void encode(GenericPacket packet, ByteBuffer buf) {
					buf.putShort((short) 0xAA55);
					buf.putShort((short) packet.getType());
					buf.putInt((Integer) packet.getData());
				}
			},
			new Encoder<GenericPacket>() {

				public Protocol getProtocol() {
					return TestProtocol.this;
				}

				public void encode(GenericPacket packet, ByteBuffer buf) {
					buf.putShort((short) 0xAA55);
					buf.putShort((short) packet.getType());
					buf.putLong((Long) packet.getData());
				}
			},
	};

	public int getPacketHeaderSize() {
		return 4;
	}

	public int getMaxPacketSize() {
		return 1024;
	}

	public boolean seekToHeader(ByteBuffer header) {
		int remaining = header.remaining();
		
		while (remaining > 1) {
			byte b = header.get();
			remaining--;
			if (b == (byte) 0xAA) {
				b = header.get();
				remaining--;
				if (b == (byte) 0x55) {
					header.position(header.position() - 2);
					return true;
				} else if (b == (byte) 0xAA) {
					header.position(header.position() - 1);
				}
			}
		}
		return false;
	}

	public int getPacketBodySize(ByteBuffer header) {
		return bodySizes[getId(header)];
	}

	public int getPacketBodySize(Packet packet) {
		GenericPacket p = (GenericPacket) packet;
		return bodySizes[p.getType()];
	}

	public Decoder<?> getPacketDecoder(ByteBuffer header) {
		return decoders[getId(header)];
	}

	public Encoder<?> getPacketEncoder(Packet packet) {
		GenericPacket p = (GenericPacket) packet;
		return encoders[p.getType()];
	}

	public Handler<?> getPacketHandler(Packet packet) {
		return null;
	}
	
	private int getId(ByteBuffer header) {
		return header.getShort(header.position() + 2);
	}

	public abstract class GenericPacket implements Packet {
		
		private final int type;
		
		public GenericPacket(int type) {
			this.type = type;
		}
		
		public int getType() {
			return type;
		}
		
		public abstract Object getData();

		public Protocol getProtocol() {
			return TestProtocol.this;
		}

		public int getStripeId() {
			return 0;
		}
	}
	
}
