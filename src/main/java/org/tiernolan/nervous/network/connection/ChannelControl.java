package org.tiernolan.nervous.network.connection;


public interface ChannelControl {

	/**
	 * Clears the write request flag. 
	 * 
	 * This method should only be called by the Serdes.write(WritableByteChannel channel) method
	 */
	public void clearWriteRequest();
	
	/**
	 * Sets the write request flag.
	 * 
	 * This method may be called by any thread
	 */
	public void setWriteRequest();
	
	/**
	 * Closes the network connection
	 */
	public void close();
	
}
