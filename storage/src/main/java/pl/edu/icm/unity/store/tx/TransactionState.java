/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.ArrayList;
import java.util.List;

import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventsBatch;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;

public class TransactionState
{
	private TransactionContext context;
	private List<RDBMSMutationEvent> rdbmsQueue = new ArrayList<>();
	
	public TransactionState(TransactionContext context)
	{
		this.context = context;
	}
	
	public TransactionState(TransactionState parent)
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