package org.tiernolan.nervous.network.connection;

import java.util.concurrent.atomic.AtomicBoolean;


public class SimpleNetwork implements ChannelControl {

	private AtomicBoolean writeRequest = new AtomicBoolean(false);
	
	public void clearWriteRequest() {
		writeRequest.compareAndSet(true, false);
	}

	public void setWriteRequest() {
		writeRequest.compareAndSet(false, true);
	}

	public boolean getWriteRequest() {
		return writeRequest.get();
	}

	public void close() {
	}
	
}
