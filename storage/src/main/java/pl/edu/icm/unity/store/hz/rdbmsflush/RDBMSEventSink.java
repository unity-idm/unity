/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.rdbmsflush;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.TransactionalQueue;
import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.base.utils.Log;
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
	
	/**
	 * TODO - this will require much more complicated logic (shutdown, demon)
	 */
	public void consumePresentAndExit()
	{
		ILock lock = hzInstance.getLock(RDBMS_EVENTS_CONSUMER_LOCK);

		lock.lock();
		log.info("This member was chosen as the RDBMS flush process");
		try
		{
			boolean hasMoreBatches;
			do
			{
				hasMoreBatches = processEvents();
			} while(hasMoreBatches);
		} finally
		{
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
			while(batch == null)
			{
				try
				{
					batch = queue.poll(120, TimeUnit.SECONDS);
				} catch (InterruptedException e)
				{
					//ok, ignore
				}
			}
			processSingleBatch(batch);
			return queue.size() > 0;
		});
	}

	private void processSingleBatch(RDBMSEventsBatch batch)
	{
		log.trace("Got RDBMS events batch for processing, size: " + batch.getEvents().size());
		rdbmsTx.runInTransaction(() -> {
			for (RDBMSMutationEvent event : batch.getEvents())
				rdbmsProcessor.apply(event, SQLTransactionTL.getSql());
		});
	}
}

