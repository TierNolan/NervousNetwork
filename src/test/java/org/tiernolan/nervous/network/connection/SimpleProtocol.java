package org.tiernolan.nervous.network.connection;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.connection.Network;
import org.tiernolan.nervous.network.api.protocol.Decoder;
import org.tiernolan.nervous.network.api.protocol.Encoder;
import org.tiernolan.nervous.network.api.protocol.Handler;
import org.tiernolan.nervous.network.api.protocol.Packet;
import org.tiernolan.nervous.network.api.protocol.Protocol;

public class SimpleProtocol implements Protocol<SimpleConnection> {
	
	private final int[] bodySizes = new int[] {4, 8, 4}; 

	@SuppressWarnings("unchecked")
	private final Decoder<GenericPacket, SimpleConnection>[] decoders = new Decoder[] {
			new Decoder<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
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
			new Decoder<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
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
			},
			new Decoder<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public GenericPacket decode(ByteBuffer header, ByteBuffer body) {
					final Integer data = body.getInt();
					return new GenericPacket(2, 1) {
						@Override
						public Object getData() {
							return data;
						}
					};
				}
			},
	};
	
	@SuppressWarnings("unchecked")
	private final Encoder<GenericPacket, SimpleConnection>[] encoders = new Encoder[] {
			new Encoder<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public void encode(GenericPacket packet, ByteBuffer buf) {
					buf.putShort((short) 0xAA55);
					buf.putShort((short) packet.getType());
					buf.putInt((Integer) packet.getData());
				}

				public int getPacketBodySize(GenericPacket packet) {
					return 4;
				}
			},
			new Encoder<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public void encode(GenericPacket packet, ByteBuffer buf) {
					buf.putShort((short) 0xAA55);
					buf.putShort((short) packet.getType());
					buf.putLong((Long) packet.getData());
				}

				public int getPacketBodySize(GenericPacket packet) {
					return 8;
				}
			},
			new Encoder<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public void encode(GenericPacket packet, ByteBuffer buf) {
					buf.putShort((short) 0xAA55);
					buf.putShort((short) packet.getType());
					buf.putInt((Integer) packet.getData());
				}

				public int getPacketBodySize(GenericPacket packet) {
					return 4;
				}
			}
	};

	@SuppressWarnings("unchecked")
	private Handler<GenericPacket, SimpleConnection>[] handlers = new Handler[] {
			new Handler<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public void handle(Connection<SimpleConnection> connection, GenericPacket packet) {
					if (Integer.valueOf(-1).equals(packet.getData())) {
						connection.getNetwork().shutdown();
					} else {
						connection.getNetwork().writePacket(packet);
					}
				}
			},
			new Handler<GenericPacket, SimpleConnection>() {

				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public void handle(Connection<SimpleConnection> connection, final GenericPacket packet) {
					connection.getNetwork().writePacket(new GenericPacket(1) {
						@Override
						public Object getData() {
							return ((Long) packet.getData()) + 1;
						}
					});
				}
			},
			new Handler<GenericPacket, SimpleConnection>() {

				private AtomicInteger last = new AtomicInteger(0);
				
				public Protocol<SimpleConnection> getProtocol() {
					return SimpleProtocol.this;
				}

				public void handle(Connection<SimpleConnection> connection, GenericPacket packet) {
					int expected = last.getAndIncrement();
					if (!packet.getData().equals(expected)) {
						connection.getNetwork().writePacket(packet);
					}
				}
			}
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
	
	@SuppressWarnings("unchecked")
	public <P extends Packet<SimpleConnection>> Decoder<P, SimpleConnection> getPacketDecoder(ByteBuffer header) {
		return (Decoder<P, SimpleConnection>) decoders[getId(header)];
	}

	@SuppressWarnings("unchecked")
	public <P extends Packet<SimpleConnection>> Encoder<P, SimpleConnection> getPacketEncoder(Packet<SimpleConnection> packet) {
		GenericPacket p = (GenericPacket) packet;
		return (Encoder<P, SimpleConnection>) encoders[p.getType()];
	}

	@SuppressWarnings("unchecked")
	public <P extends Packet<SimpleConnection>> Handler<P, SimpleConnection> getPacketHandler(Packet<SimpleConnection> packet) {
		return (Handler<P, SimpleConnection>) handlers[((GenericPacket) packet).getType()];
	}

	public int getPacketBodySize(ByteBuffer header) {
		return bodySizes[getId(header)];
	}

	public int getPacketBodySize(Packet<SimpleConnection> packet) {
		GenericPacket p = (GenericPacket) packet;
		return bodySizes[p.getType()];
	}

	private int getId(ByteBuffer header) {
		return header.getShort(header.position() + 2);
	}

	public abstract class GenericPacket implements Packet<SimpleConnection> {
		
		private final int type;
		private final int stripeId;
		
		public GenericPacket(int type) {
			this(type, 0);
		}
		public GenericPacket(int type, int stripeId) {
			this.type = type;
			this.stripeId = stripeId;
		}
		
		public int getType() {
			return type;
		}
		
		public abstract Object getData();

		public Protocol<SimpleConnection> getProtocol() {
			return SimpleProtocol.this;
		}

		public int getStripeId() {
			return stripeId;
		}
		
		public String toString() {
			Object data = getData();
			if (data instanceof Integer) {
				return Integer.toHexString((Integer) data);
			} else if (data instanceof Long) {
				return Long.toHexString((Long) data);
			} else {
				return data.toString();
			}
		}
	}

	public SimpleConnection newConnection(Network<SimpleConnection> network) {
		return new SimpleConnection(this, network);
	}
	
}
