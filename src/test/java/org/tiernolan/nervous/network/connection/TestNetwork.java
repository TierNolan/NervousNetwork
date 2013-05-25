package org.tiernolan.nervous.network.connection;

import java.util.concurrent.atomic.AtomicBoolean;


public class TestNetwork implements Network {

	private AtomicBoolean writeRequest = new AtomicBoolean(false);
	
	public boolean clearWriteRequest() {
		return writeRequest.compareAndSet(true, false);
	}

	public boolean setWriteRequest() {
		return writeRequest.compareAndSet(false, true);
	}

	public boolean getWriteRequest() {
		return writeRequest.get();
	}
	
}
