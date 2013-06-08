package org.tiernolan.nervous.network.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.tiernolan.nervous.network.api.queue.Striped;

public class StripedMergingQueueTest {
	
	
	@Test
	public void testStripes() throws InterruptedException {
		
		StripedMergingQueue<StripedObject> master = new StripedMergingQueue<StripedObject>();
		
		StripedQueueImpl<StripedObject> q1 = new StripedQueueImpl<StripedObject>(master);

		q1.offer(new StripedObject(1, 1));

		q1.offer(new StripedObject(2, 1));

		q1.offer(new StripedObject(3, 2));
		
		Completable<StripedObject> s1 = master.take();
		
		assertEquals("Wrong object retrieved", s1.getStriped().getId(), 1);
		
		Completable<StripedObject> s3 = master.take();
		
		assertEquals("Wrong object retrieved", s3.getStriped().getId(), 3);
		
		Completable<StripedObject> s2 = master.poll();
		
		assertNull("No object should have been available, since stripe 1 was not done", s2);

		s1.done();
		
		s2 = master.poll();
		
		assertEquals("Wrong object retrieved", s2.getStriped().getId(), 2);
		
	}
	
	@Test
	public void testAsync() throws InterruptedException {
		
		StripedMergingQueue<StripedObject> master = new StripedMergingQueue<StripedObject>();
		
		StripedQueueImpl<StripedObject> q = new StripedQueueImpl<StripedObject>(master);

		q.offer(new StripedObject(1, -1));

		q.offer(new StripedObject(2, -1));

		q.offer(new StripedObject(3, -1));
		
		Completable<StripedObject> s1 = master.take();
		
		assertEquals("Wrong object retrieved", s1.getStriped().getId(), 1);

		Completable<StripedObject> s2 = master.take();
		
		assertEquals("Wrong object retrieved", s2.getStriped().getId(), 2);

		Completable<StripedObject> s3 = master.poll();
		
		assertEquals("Wrong object retrieved", s3.getStriped().getId(), 3);
		
	}
	
	@Test
	public void randomTest() {
		
		StripedMergingQueue<StripedObject> master = new StripedMergingQueue<StripedObject>();
		
		StripedQueueImpl<StripedObject> q1 = new StripedQueueImpl<StripedObject>(master);
		StripedQueueImpl<StripedObject> q2 = new StripedQueueImpl<StripedObject>(master);
		
		@SuppressWarnings("unchecked")
		StripedQueueImpl<StripedObject>[] q = new StripedQueueImpl[] {q1, q2};
		
		int[][] lastId = new int[2][16];
		
		ArrayList<Completable<StripedObject>> retrieved = new ArrayList<Completable<StripedObject>>();
		
		HashSet<StripedObject> objects = new HashSet<StripedObject>();
		
		Random r = new Random();
		
		int id = 1;
		for (int i = 0; i < 1000 || !objects.isEmpty(); i++) {
			
			int stripe = r.nextInt(17) - 1;
			
			int queueId = r.nextInt(2);
			
			StripedObject o = new StripedObject(id++, stripe, queueId);
			
			if (i < 1000) {
				q[queueId].offer(o);

				objects.add(o);
			}
			
			if (retrieved.size() > 16 || (i >= 1000 && retrieved.size() > 0)) {
				Completable<StripedObject> c = retrieved.remove(r.nextInt(retrieved.size()));
				c.done();
			}

			Completable<StripedObject> c = master.poll();

			if (c != null) {
				retrieved.add(c);
				if (c.getStripeId() != -1) {
					assertTrue("Element out of order for stripe " + c.getStripeId(), c.getStriped().getId() > lastId[c.getStriped().getQueueId()][c.getStripeId()]);
					lastId[c.getStriped().getQueueId()][c.getStripeId()] = c.getStriped().getId();
				}
				assertTrue("Unable to remove object", objects.remove(c.getStriped()));
			}
		}
		
		assertTrue("Queue 1 not empty", q1.isEmptyRaw());
		assertTrue("Queue 2 not empty", q2.isEmptyRaw());
		assertTrue("Queue not empty", master.isEmpty());
		
	}
	
	@Test
	public void testBlocking() throws InterruptedException {
		
		final StripedMergingQueue<StripedObject> master = new StripedMergingQueue<StripedObject>();
		
		final StripedQueueImpl<StripedObject> q = new StripedQueueImpl<StripedObject>(master);

		q.offer(new StripedObject(1, 1));

		q.offer(new StripedObject(2, 1));

		q.offer(new StripedObject(3, 2));
		
		Completable<StripedObject> s1 = master.poll();
		
		assertEquals("Wrong object retrieved", s1.getStriped().getId(), 1);
		
		Completable<StripedObject> s3 = master.poll();
		
		assertEquals("Wrong object retrieved", s3.getStriped().getId(), 3);
		
		final AtomicReference<Throwable> asyncException = new AtomicReference<Throwable>();
		
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					Completable<StripedObject> s2 = master.take();
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
					Completable<StripedObject> s4 = master.take();
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
		
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				try {
					Completable<StripedObject> s4 = master.take();
					int s = s4.getStriped().getId();
					assertTrue("Wrong object retrieved", s == 6 || s == 7);
					s4.done();
				} catch (Throwable e) {
					asyncException.set(e);
				}
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				try {
					Completable<StripedObject> s4 = master.take();
					int s = s4.getStriped().getId();
					assertTrue("Wrong object retrieved", s == 6 || s == 7);
					s4.done();
				} catch (Throwable e) {
					asyncException.set(e);
				}
			}
		});

		StripedObject s = new StripedObject(5, 4);
		q.offer(s);
		
		q.offer(new StripedObject(6, 4));
		q.offer(new StripedObject(7, 4));
		
		Completable<StripedObject> c = master.poll();
		
		assertEquals("Retrieved wrong object", c.getStriped(), s);

		t1.start();
		t2.start();
		
		Thread.sleep(2);
		
		assertTrue("Async take() did not block when queue empty", t1.isAlive());
		assertTrue("Async take() did not block when queue empty", t2.isAlive());
		
		c.done();
		
		Thread.sleep(2);
		
		assertTrue("done() did not release block", !t1.isAlive());
		assertTrue("done() did not release block", !t2.isAlive());
		
		if (asyncException.get() != null) {
			throw new RuntimeException("Async exception thrown", asyncException.get());
		}
		
	}
	
	@Test
	public void multiThreadTest() throws InterruptedException {
		
		WriterThread[] writers = new WriterThread[8];
		ReaderThread[] readers = new ReaderThread[8];
		
		StripedMergingQueue<StripedObject> master = new StripedMergingQueue<StripedObject>();
		
		for (int i = 0; i < 8; i++) {
			writers[i] = new WriterThread(master, 500, i);
			readers[i] = new ReaderThread(master);
		}
		
		for (int i = 0; i < 8; i++) {
			readers[i].start();
		}

		for (int i = 0; i < 8; i++) {
			writers[i].start();
		}
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < 8; i++) {
			long delay = start + 1000 - System.currentTimeMillis();
			if (delay > 0) {
				writers[i].join(delay);
			}
		}
		
		for (int i = 0; i < 8; i++) {
			assertTrue("Writer threads had not shutdown", !writers[i].isAlive());
		}
		
		for (int i = 0; i < 8; i++) {
			assertTrue("Reader thread shutdown", readers[i].isAlive());
		}
		
		while (getRead(readers) < 500 * 8) {
			Thread.sleep(50);
		}
		
		for (int i = 0; i < 8; i++) {
			assertTrue("Reader thread shutdown", readers[i].isAlive());
		}
		
		for (int i = 0; i < 8; i++) {
			readers[i].interrupt();
		}
		
		start = System.currentTimeMillis();
		for (int i = 0; i < 8; i++) {
			long delay = start + 1000 - System.currentTimeMillis();
			if (delay > 0) {
				readers[i].join(delay);
			}
		}
		
		for (int i = 0; i < 8; i++) {
			assertTrue("Reader thread failed to shutdown after interrupt", !readers[i].isAlive());
		}
		
	}
	
	private int getRead(ReaderThread[] readers) {
		int read = 0;
		for (int i = 0; i < readers.length; i++) {
			assertTrue("Fault detected " + readers[i].getFailed(), readers[i].getFailed() == null);
			read += readers[i].read;
		}
		return read;
	}
	
	private static class StripedObject implements Striped {

		private static AtomicInteger count = new AtomicInteger(1);
		
		private final int hashCode;
		private final int id;
		private final int stripe;
		private final int queueId;
		
		public StripedObject(int id, int stripe) {
			this(id, stripe, 0);
		}
		
		public StripedObject(int id, int stripe, int queueId) {			
			this.id = id;
			this.stripe = stripe;
			this.hashCode = count.getAndIncrement();
			this.queueId = queueId;
		}
		
		public int getStripeId() {
			return stripe;
		}
		
		public int getId() {
			return id;
		}
		
		public int getQueueId() {
			return queueId;
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
	
	private static class WriterThread extends Thread {
		
		private final StripedQueueImpl<StripedObject> q;
		private final int count;
		private final int queueId;
		
		WriterThread(StripedMergingQueue<StripedObject> master, int count, int queueId) {
			this.q = new StripedQueueImpl<StripedObject>(master);
			this.count = count;
			this.queueId = queueId;
		}
		
		public void run() {
			Random r = new Random();
			for (int i = 0; i < count; i++) {
				q.offer(new StripedObject(i, r.nextInt(16), queueId));
			}
		}
		
	}
	
	private static class ReaderThread extends Thread {
		
		private static final AtomicBoolean[][] inUse = new AtomicBoolean[8][16];
		private static final AtomicInteger[][] lastId = new AtomicInteger[8][16];
		
		static {
			for (int j = 0; j < inUse.length; j++) {
				AtomicBoolean[] b = inUse[j];
				AtomicInteger[] i = lastId[j];
				for (int k = 0; k < b.length; k++) {
					b[k] = new AtomicBoolean(false);
					i[k] = new AtomicInteger(-1);
				}
			}
		}
		
		private final StripedQueue<StripedObject> master;
		private volatile String failed = null;
		private int read;
		
		public ReaderThread(StripedQueue<StripedObject> master) {
			this.master = master;
		}
		
		public String getFailed() {
			return failed;
		}
		
		public int getRead() {
			return read;
		}
		
		public void run() {
			
			while (!interrupted()) {
				
				try {
					Completable<StripedObject> o = master.take();
					int stripe = o.getStripeId();
					int queueId = o.getStriped().getQueueId();
					try {
						if (!inUse[queueId][stripe].compareAndSet(false, true)) {
							failed = "Two accesses to the same stripe/queue pair happened at once";
							break;
						}
						int id = o.getStriped().getId();
						int l = lastId[queueId][stripe].getAndSet(id);
						if (id <= l) {
							failed = "Striped packets out of order";
							break;
						}
					} finally {
						inUse[queueId][stripe].set(false);
						o.done();
						read++;
					}
				} catch (InterruptedException e) {
					break;
				}
				
			}
			
		}
		
	}

}
