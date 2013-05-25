package org.tiernolan.nervous.network.map;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDualKeyHashMap<P, S, V> {
	
	private final ConcurrentHashMap<P, DualKeyEntry> primaryMap = new ConcurrentHashMap<P, DualKeyEntry>();
	private final ConcurrentHashMap<S, DualKeyEntry> secondaryMap = new ConcurrentHashMap<S, DualKeyEntry>();
	private final Set<P> primaryKeySet = primaryMap.keySet();

	/**
	 * Puts the double key to value pair into the map.<br>
	 * <br>
	 * A secondary key may not be associated with a more than one primary key.
	 * 
	 * @param primary
	 * @param secondary
	 * @param value
	 * @return
	 */
	public V put(P primary, S secondary, V value) {
		
		if (primary == null || secondary == null || value == null) {
			throw new NullPointerException();
		}
		
		DualKeyEntry entry = new DualKeyEntry(secondary, value);
		
		DualKeyEntry old = primaryMap.put(primary, entry);

		if (old == null) {
			transitionSecondary(primary, secondary, null, entry, entry, null);
		} else if (old.getSecondary().equals(secondary)) {
			transitionSecondary(primary, secondary, old, entry, entry, old);
		} else {
			transitionSecondary(primary, old.getSecondary(), old, null, entry, old);
			transitionSecondary(primary, secondary, null, entry, entry, old);
		}

		if (old == null) {
			return null;
		} else {
			return old.getValue();
		}
		
	}
	
	/**
	 * Puts the double key to value pair into the map, if the primary key is not already mapped to a value.<br>
	 * <br>
	 * A secondary key may not be associated with a more than one primary key.
	 * 
	 * @param primary
	 * @param secondary
	 * @param value
	 * @return
	 */
	public V putIfAbsent(P primary, S secondary, V value) {
		
		if (secondary == null || value == null) {
			throw new NullPointerException();
		}
		
		DualKeyEntry entry = new DualKeyEntry(secondary, value);
		
		DualKeyEntry old = primaryMap.putIfAbsent(primary, entry);
		
		if (old != null) {
			return old.getValue();
		}

		transitionSecondary(primary, secondary, null, entry, entry, null);

		return null;
	}
	
	/**
	 * Removes the value associated with the primary key from the map.<br>
	 * 
	 * @param primary
	 * @return
	 */
	public V remove(P primary) {
		DualKeyEntry old = primaryMap.remove(primary);
		
		if (old == null) {
			return null;
		}
		
		boolean done = false;
		while (!done) {
			done = secondaryMap.remove(old.getSecondary(), old);
			if (!done) {
				Thread.yield();
			}
		}
		
		return old.getValue();
	}
	
	/**
	 * Gets the value associated with a primary key
	 * 
	 * @param key
	 * @return the value, or null
	 */
	public V get(P key) {
		DualKeyEntry entry = primaryMap.get(key);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}
	
	/**
	 * Gets the value associated with a secondary key
	 * 
	 * @param key
	 * @return the value, or null
	 */
	public V getSecondary(S key) {
		DualKeyEntry entry = secondaryMap.get(key);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}
	
	/**
	 * Gets the set of primary keys
	 * 
	 * @return
	 */
	public Set<P> getPrimaryKeySet() {
		return primaryKeySet;
	}
	
	private int checkCollision(int i) {
		if (i < 100) {
			return i + 1;
		}
		HashMap<S, P> map = new HashMap<S, P>();
		for (Entry<P,  DualKeyEntry> entry: primaryMap.entrySet()) {
			P old = map.put(entry.getValue().getSecondary(), entry.getKey());
			if (old != null) {
				throw new IllegalStateException("Secondary key " + entry.getValue().getSecondary() + " is associated with 2 primary keys " + old + " and " + entry.getKey());
			}
		}
		return 0;
	}
	
	private void transitionSecondary(P primary, S secondary, DualKeyEntry from, DualKeyEntry to, DualKeyEntry revertFrom, DualKeyEntry revertTo) {
		int i = 0;
		boolean done = false;
		while (!done) {
			if (from == null) {
				done = secondaryMap.putIfAbsent(secondary, to) == null;
			} else if (to == null) {
				done = secondaryMap.remove(secondary, from);
			} else {
				done = secondaryMap.replace(secondary, from, to);
			}
			if (!done) {
				Thread.yield();
				try {
					i = checkCollision(i);
				} catch (IllegalStateException e) {
					if (revertTo == null) {
						if (!primaryMap.remove(primary, revertFrom)) {
							throw new IllegalStateException("Unable to revert illegal duplicate secondary key operation", e);
						}
					} else {
						if (!primaryMap.replace(primary, revertFrom, revertTo)) {
							throw new IllegalStateException("Unable to revert illegal duplicate secondary key operation", e);
						}
					}
					throw e;
				}
			}
		}
	}

	private class DualKeyEntry {
		
		private final S secondary;
		private final V value;
		
		public DualKeyEntry(S secondary, V value) {
			this.secondary = secondary;
			this.value = value;
		}
		
		public S getSecondary() {
			return secondary;
		}
		
		public V getValue() {
			return value;
		}
		
	}
	
}
