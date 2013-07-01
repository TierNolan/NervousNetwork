package org.tiernolan.nervous.network.api.connection;

import org.tiernolan.nervous.network.api.protocol.ProtocolComponent;

public interface Connection<C extends Connection<C>> extends ProtocolComponent<C>, NetworkComponent<C> {

}
