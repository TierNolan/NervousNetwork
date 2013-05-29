package org.tiernolan.nervous.network.connection;

public interface Network {

	/**
	 * Clears the write request flag
	 * 
	 * @return true if the flag was cleared
	 */
	public boolean clearWriteRequest();
	
	/**
	 * Sets the write request flag
	 * 
	 * @return true if the flag was set
	 */
	public boolean setWriteRequest();
	
}
