package org.tiernolan.nervous.network.connection;

import org.tiernolan.nervous.network.api.connection.Connection;
import org.tiernolan.nervous.network.api.connection.Network;
import org.tiernolan.nervous.network.api.protocol.Protocol;

public class SimpleConnection implements Connection<SimpleConnection> {
	
	private final Protocol<SimpleConnection> protocol;
	private final Network<SimpleConnection> network;
	
	public SimpleConnection(Protocol<SimpleConnection> protocol, Network<SimpleConnection> network) {
		this.protocol = protocol;
		this.network = network;
	}

	public Protocol<SimpleConnection> getProtocol() {
		return protocol;
	}

	public Network<SimpleConnection> getNetwork() {
		return network;
	}

}
