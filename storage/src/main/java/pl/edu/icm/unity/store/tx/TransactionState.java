/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import java.util.ArrayList;
import java.util.List;

import com.hazelcast.transaction.TransactionContext;

import pl.edu.icm.unity.store.RDBMSEventsBatch;
import pl.edu.icm.unity.store.RDBMSMutationEvent;
import pl.edu.icm.unity.store.api.tx.Propagation;

public class TransactionState
{
	private Propagation propagation;
	private TransactionContext context;
	private List<RDBMSMutationEvent> rdbmsQueue = new ArrayList<>();
	
	public TransactionState(Propagation propagation, TransactionContext context)
	{
		this.propagation = propagation;
		this.context = context;
	}
	
	public TransactionState(Propagation propagation, TransactionState parent)
	{
		this.propagation = propagation;
		this.context = parent.context;
		this.rdbmsQueue = parent.rdbmsQueue;
	}
	
	public Propagation getPropagation()
	{
		return propagation;
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