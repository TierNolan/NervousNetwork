package org.tiernolan.nervous.network.api;

import java.util.logging.Logger;

import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface NetworkManager extends ProtocolComponent {

	public Logger getLogger();
	
}
