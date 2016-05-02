/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz.tx;

import java.util.ArrayList;
import java.util.List;

import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSEventsBatch;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;

public class HzTransactionState
{
	private TransactionContext context;
	private List<RDBMSMutationEvent> rdbmsQueue = new ArrayList<>();
	
	public HzTransactionState(TransactionContext context)
	{
		this.context = context;
	}
	
	public HzTransactionState(HzTransactionState parent)
	{
		this.context = parent.context;
		this.rdbmsQueue = parent.rdbmsQueue;
	}
	
	public void enqueueEvent(RDBMSMutationEvent event)
	{
		rdbmsQueue.add(event);
	}
	
	public RDBMSEventsBatch getBatch()
	{
		return new RDBMSEventsBatch(rdbmsQueue);
	}
	
	public TransactionContext getHzContext()
	{
		return context;
	}
}