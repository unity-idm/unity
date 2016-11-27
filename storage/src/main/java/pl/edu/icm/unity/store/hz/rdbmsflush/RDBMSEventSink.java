/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.rdbmsflush;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.TransactionalQueue;
import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;

/**
 * Background thread flushing RBMS mutation events from the global queue to RDBMS.
 * Only one such thread is really working in a cluster so we have linear ordering of RDBMS transactions.
 * 
 * @author K. Benedyczak
 */
@Component
public class RDBMSEventSink
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, RDBMSEventSink.class);
	
	public static final String RDBMS_EVENTS_CONSUMER_LOCK = "rdbmsEventsConsumerLock";
	public static final String RDBMS_EVENTS_QUEUE = "rdbmsEventsQueue";
	public static final String INNER_WAIT_LOCK = "rdbmsEventsConsumerInnerLock";
	public static final String INNER_WAIT_CONDITION = "rdbmsEventsConsumerInnerCondition";

	@Autowired
	private HazelcastInstance hzInstance;
	@Autowired @Qualifier(HzTransactionalRunner.NAME)
	private TransactionalRunner hztx;
	@Autowired @Qualifier(SQLTransactionalRunner.NAME)
	private TransactionalRunner rdbmsTx;
	@Autowired
	private RDBMSMutationEventProcessor rdbmsProcessor;

	private volatile AtomicBoolean stopped = new AtomicBoolean(false);
	private volatile CountDownLatch latch = new CountDownLatch(0);
	private volatile AtomicBoolean working = new AtomicBoolean(false);
	private Thread flushThread;

	public RDBMSEventSink()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
		flushThread = new Thread();
	}
	
	public void start()
	{
		if (working.get())
			throw new IllegalStateException("Can not start events sink while it is already started");
		stopped.set(false);
		latch = new CountDownLatch(1);
		flushThread = new Thread(() -> awaitAndConsume(), "Hazelcast to RDBMS flush");
		flushThread.start();
	}
	
	public void stop()
	{
		log.info("Stopping flush thread");
		stopped.set(true);
		flushThread.interrupt();
		while (working.get())
		{
			try
			{
				log.debug("Awaiting for termination");
				latch.await();
				break;
			} catch (InterruptedException e) {}
		}
		log.debug("Flush thread termiantion await finished");
	}
	
	private void awaitAndConsume()
	{
		ILock lock = hzInstance.getLock(RDBMS_EVENTS_CONSUMER_LOCK);
		lock.lock();
		working.set(true);
		log.info("This member was chosen as the RDBMS flush process");
		try
		{
			boolean hasMore;
			do
			{
				hasMore = processEvents();
			} while(!stopped.get() || hasMore);
		} finally
		{
			log.debug("Flush thread is being stopped");
			working.set(false);
			latch.countDown();
			try
			{
				lock.unlock();
			} catch (Exception e)
			{
				log.debug("Unlocking distributed flush lock failed, "
						+ "typically this is fine as we are exiting", e);
			}
			log.info("This member is exiting and won't flush to RDBMS anymore");
		}
	}

	public void consumePresentAndExit()
	{
		ILock lock = hzInstance.getLock(RDBMS_EVENTS_CONSUMER_LOCK);
		lock.lock();
		working.set(true);
		try
		{
			boolean hasMore;
			do
			{
				hasMore = processEvents();
			} while(hasMore && !stopped.get());
		} finally
		{
			latch.countDown();
			working.set(false);
			lock.unlock();
		}
	}

	/**
	 * Executed only by a single thread in the cluster. Waits for notification on non empty queue 
	 * (it has also safety measure to recheck the queue from time to time manually) and if some workload is found
	 * proceeds with operation.
	 * @return true if there are more batches in the queue
	 */
	private boolean processEvents()
	{
		return hztx.runInTransactionRet(() -> {
			TransactionContext hzContext = HzTransactionTL.getHzContext();
			TransactionalQueue<RDBMSEventsBatch> queue = hzContext.getQueue(RDBMS_EVENTS_QUEUE);
			
			RDBMSEventsBatch batch = null;
			while(batch == null && !stopped.get())
			{
				try
				{
					batch = queue.poll(2, TimeUnit.SECONDS);
				} catch (HazelcastException | InterruptedException e)
				{
					log.debug("Got Interrupt");
					return queue.size() > 0;
				}
			}
			if (batch != null)
				processSingleBatch(batch);
			return queue.size() > 0;
		});
	}

	private void processSingleBatch(RDBMSEventsBatch batch)
	{
		log.trace("Got RDBMS events batch for processing, size: {}", batch.getEvents().size());
		rdbmsTx.runInTransaction(() -> {
			for (RDBMSMutationEvent event : batch.getEvents())
				rdbmsProcessor.apply(event, SQLTransactionTL.getSql());
		});
	}
}

