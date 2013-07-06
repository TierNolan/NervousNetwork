package org.tiernolan.nervous.network.connection;

import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.connection.Network;
import org.tiernolan.nervous.network.api.protocol.Protocol;

public class SimpleConnection extends Connection<SimpleConnection> {
	
	private final Protocol<SimpleConnection> protocol;
	
	public SimpleConnection(Protocol<SimpleConnection> protocol, Network<SimpleConnection> network) {
		super(network);
		this.protocol = protocol;
	}

	public Protocol<SimpleConnection> getProtocol() {
		return protocol;
	}

}
