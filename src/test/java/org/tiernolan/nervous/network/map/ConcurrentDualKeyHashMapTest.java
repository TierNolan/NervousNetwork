package org.tiernolan.nervous.network.map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Random;

import org.junit.Test;

public class ConcurrentDualKeyHashMapTest {
	
	@Test
	public void testCollisionFromNull() {
		
		ConcurrentDualKeyHashMap<Integer, Long, Short> map = new ConcurrentDualKeyHashMap<Integer, Long, Short>();
		
		map.put(1, 2L, (short) 8);
		
		boolean thrown = false;
		try {
			map.put(2, 2L, (short) 6);
		} catch (IllegalStateException e) {
			thrown = true;
		}
		
		assertTrue("Collision not detected", thrown);
		
	}
	
	@Test
	public void testCollisionFromMapped() {
		
		ConcurrentDualKeyHashMap<Integer, Long, Short> map = new ConcurrentDualKeyHashMap<Integer, Long, Short>();
		
		map.put(1, 2L, (short) 8);
		
		map.put(2, 7L, (short) 6);
		
		boolean thrown = false;
		try {
			map.put(2, 2L, (short) 6);
		} catch (IllegalStateException e) {
			thrown = true;
		}
		
		assertTrue("Collision not detected", thrown);
		
	}
	
	@Test
	public void testCollisionFromIfAbsent() {
		
		ConcurrentDualKeyHashMap<Integer, Long, Short> map = new ConcurrentDualKeyHashMap<Integer, Long, Short>();
		
		map.put(1, 2L, (short) 8);
		
		boolean thrown = false;
		try {
			map.putIfAbsent(2, 2L, (short) 6);
		} catch (IllegalStateException e) {
			thrown = true;
		}
		
		assertTrue("Collision not detected", thrown);
		
	}
	
	@Test
	public void randomTest() {
		
		ConcurrentDualKeyHashMap<Integer, Long, Short> map = new ConcurrentDualKeyHashMap<Integer, Long, Short>();
		
		HashMap<Integer, Short> primaryMap = new HashMap<Integer, Short>();

		HashMap<Long, Short> secondaryMap = new HashMap<Long, Short>();
		
		@SuppressWarnings("unchecked")
		DualKeyEntry<Integer, Long, Short>[] entries = new DualKeyEntry[1024];
		
		Random r = new Random();
		
		for (int i = 0; i < entries.length; i++) {
			entries[i] = new DualKeyEntry<Integer, Long, Short>(r.nextInt(), r.nextLong(), (short) r.nextInt());
		}
		
		for (int i = 0; i < entries.length * 8; i++) {
			if (r.nextBoolean()) {
				int id = r.nextInt(entries.length);
				
				DualKeyEntry<Integer, Long, Short> e = entries[id];
				
				Short primaryValue = primaryMap.put(e.getPrimary(), e.getValue());
				Short secondaryValue = secondaryMap.put(e.getSecondary(), e.getValue());
				Short mapValue = map.put(e.getPrimary(), e.getSecondary(), e.getValue());

				assertEquals("Primary value map did not match dual map", primaryValue, mapValue);
				assertEquals("Secondary value map did not match dual map", secondaryValue, mapValue);
			}
			if (r.nextBoolean()) {
				int id = r.nextInt(entries.length);
				
				DualKeyEntry<Integer, Long, Short> e = entries[id];

				Short mapValue = map.putIfAbsent(e.getPrimary(), e.getSecondary(), e.getValue());
				
				if (mapValue == null) {
					Short primaryValue = primaryMap.put(e.getPrimary(), e.getValue());
					Short secondaryValue = secondaryMap.put(e.getSecondary(), e.getValue());

					assertEquals("Primary value map did not match dual map", primaryValue, mapValue);
					assertEquals("Secondary value map did not match dual map", secondaryValue, mapValue);
				} else {
					assertTrue("Failed to add entry to dual map, even though key mapping was absent", primaryMap.containsKey(e.getPrimary()));
				}
			}
			if (r.nextBoolean()) {
				int id = r.nextInt(entries.length);
				
				DualKeyEntry<Integer, Long, Short> e = entries[id];
				
				Short primaryValue = primaryMap.remove(e.getPrimary());
				Short secondaryValue = secondaryMap.remove(e.getSecondary());
				Short mapValue = map.remove(e.getPrimary());
				
				
				assertEquals("Primary value map did not match dual map", primaryValue, mapValue);
				assertEquals("Secondary value map did not match dual map", secondaryValue, mapValue);
			}
		}
		
		
		
	}
	
	private static class DualKeyEntry<P, S, V> {
		
		private final P primary;
		private final S secondary;
		private final V value;
		
		public DualKeyEntry(P primary, S secondary, V value) {
			this.primary = primary;
			this.secondary = secondary;
			this.value = value;
		}
		
		public S getSecondary() {
			return secondary;
		}
		
		public V getValue() {
			return value;
		}
		
		public P getPrimary() {
			return primary;
		}
		
	}

}
