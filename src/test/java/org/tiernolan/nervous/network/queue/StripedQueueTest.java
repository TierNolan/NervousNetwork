package org.tiernolan.nervous.network.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.tiernolan.nervous.network.api.queue.Striped;

public class StripedQueueTest {
	
	
	@Test
	public void testStripes() throws InterruptedException {
		
		StripedQueue<StripedObject> q = new StripedQueue<StripedObject>();

		q.offer(new StripedObject(1, 1));

		q.offer(new StripedObject(2, 1));

		q.offer(new StripedObject(3, 2));
		
		CompletableStriped<StripedObject> s1 = q.take();
		
		assertEquals("Wrong object retrieved", s1.getStriped().getId(), 1);

		CompletableStriped<StripedObject> s3 = q.take();
		
		assertEquals("Wrong object retrieved", s3.getStriped().getId(), 3);

		CompletableStriped<StripedObject> s2 = q.poll();
		
		assertNull("No object should have been available, since stripe 1 was not done", s2);

		s1.done();
		
		s2 = q.poll();
		
		assertEquals("Wrong object retrieved", s2.getStriped().getId(), 2);
		
	}
	
	@Test
	public void testAsync() throws InterruptedException {
		
		StripedQueue<StripedObject> q = new StripedQueue<StripedObject>();

		q.offer(new StripedObject(1, -1));

		q.offer(new StripedObject(2, -1));

		q.offer(new StripedObject(3, -1));
		
		CompletableStriped<StripedObject> s1 = q.take();
		
		assertEquals("Wrong object retrieved", s1.getStriped().getId(), 1);

		CompletableStriped<StripedObject> s2 = q.take();
		
		assertEquals("Wrong object retrieved", s2.getStriped().getId(), 2);

		CompletableStriped<StripedObject> s3 = q.poll();
		
		assertEquals("Wrong object retrieved", s3.getStriped().getId(), 3);
		
	}
	
	@Test
	public void randomTest() {
		
		StripedQueue<StripedObject> q = new StripedQueue<StripedObject>();
		
		int[] lastId = new int[16];
		
		ArrayList<CompletableStriped<StripedObject>> retrieved = new ArrayList<CompletableStriped<StripedObject>>();
		
		HashSet<StripedObject> objects = new HashSet<StripedObject>();
		
		Random r = new Random();
		
		int id = 1;
		for (int i = 0; i < 1000 || !objects.isEmpty(); i++) {
			
			int stripe = r.nextInt(17) - 1;
			
			StripedObject o = new StripedObject(id++, stripe);
			
			if (i < 1000) {
				q.offer(o);

				objects.add(o);
			}
			
			if (retrieved.size() > 16 || (i >= 1000 && retrieved.size() > 0)) {
				CompletableStriped<StripedObject> c = retrieved.remove(r.nextInt(retrieved.size()));
				c.done();
			}

			CompletableStriped<StripedObject> c = q.poll();
			
			if (c != null) {
				retrieved.add(c);
				if (c.getStripeId() != -1) {
					assertTrue("Element out of order for stripe " + c.getStripeId(), c.getStriped().getId() > lastId[c.getStripeId()]);
					lastId[c.getStripeId()] = c.getStriped().getId();
				}
				assertTrue("Unable to remove object", objects.remove(c.getStriped()));
			}
		}
		
		assertTrue("Queue not empty", q.isEmpty());
		
	}
	
	@Test
	public void testBlocking() throws InterruptedException {
		
		final StripedQueue<StripedObject> q = new StripedQueue<StripedObject>();

		q.offer(new StripedObject(1, 1));

		q.offer(new StripedObject(2, 1));

		q.offer(new StripedObject(3, 2));
		
		CompletableStriped<StripedObject> s1 = q.poll();
		
		assertEquals("Wrong object retrieved", s1.getStriped().getId(), 1);
		
		CompletableStriped<StripedObject> s3 = q.poll();
		
		assertEquals("Wrong object retrieved", s3.getStriped().getId(), 3);
		
		final AtomicReference<Throwable> asyncException = new AtomicReference<Throwable>();
		
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					CompletableStriped<StripedObject> s2 = q.take();
					assertEquals("Wrong object retrieved", s2.getStriped().getId(), 2);
					s2.done();
				} catch (Throwable e) {
					asyncException.set(e);
				}
			}
		});
		
		t.start();
		
		Thread.sleep(2);
		
		assertTrue("Async take() did not block when stripe had not done", t.isAlive());
		
		s1.done();
		
		Thread.sleep(2);
		
		assertTrue("Async done() did not release block", !t.isAlive());
		
		t.join();
		
		t = new Thread(new Runnable() {
			public void run() {
				try {
					CompletableStriped<StripedObject> s4 = q.take();
					assertEquals("Wrong object retrieved", s4.getStriped().getId(), 4);
				} catch (Throwable e) {
					asyncException.set(e);
				}
			}
		});
		
		t.start();
		
		Thread.sleep(2);
		
		assertTrue("Async take() did not block when queue empty", t.isAlive());
		
		q.offer(new StripedObject(4, 1));
		
		Thread.sleep(2);
		
		assertTrue("offer() did not release block", !t.isAlive());
		
		if (asyncException.get() != null) {
			throw new RuntimeException("Async exception thrown", asyncException.get());
		}
		
	}
	
	
	private static class StripedObject implements Striped {

		private static int count = 0;
		
		private final int hashCode;
		private final int id;
		private final int stripe;
		
		public StripedObject(int id, int stripe) {
			this.id = id;
			this.stripe = stripe;
			this.hashCode = count++;
		}
		
		public int getStripeId() {
			return stripe;
		}
		
		public int getId() {
			return id;
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			} else if (!(o instanceof StripedObject)) {
				return false;
			} else {
				return ((StripedObject) o).hashCode == hashCode;
			}
		}
		
		@Override
		public String toString() {
			return "{id=" + id + ", stripe=" + stripe + "}";
		}
		
	}

}