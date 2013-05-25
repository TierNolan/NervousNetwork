package org.tiernolan.nervous.network.bufferpool;

import java.lang.ref.SoftReference;

public class FlexibleReference<T> extends SoftReference<T> {
	
	private T hardReference;

	public FlexibleReference(T ref, boolean hard) {
		super(ref);
		this.hardReference = hard ? ref : null;
	}
	
	public boolean setHard() {
		hardReference = super.get();
		return hardReference != null;
	}
	
	public void setSoft() {
		hardReference = null;
	}
	
}
