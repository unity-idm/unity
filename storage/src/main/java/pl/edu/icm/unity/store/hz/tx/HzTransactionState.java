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
import pl.edu.icm.unity.store.tx.TransactionState;

public class HzTransactionState implements TransactionState
{
	private TransactionContext context;
	private List<RDBMSMutationEvent> rdbmsQueue = new ArrayList<>();
	private Runnable transactionCommiter;
	private Runnable resetHandler;
	
	public HzTransactionState(TransactionContext context, Runnable transactionCommiter, Runnable resetHandler)
	{
		this.context = context;
		this.transactionCommiter = transactionCommiter;
		this.resetHandler = resetHandler;
	}
	
	public HzTransactionState(HzTransactionState parent)
	{
		this.context = parent.context;
		this.rdbmsQueue = parent.rdbmsQueue;
		this.transactionCommiter = parent.transactionCommiter;
		this.resetHandler = parent.resetHandler;
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
	
	@Override
	public void manualCommit()
	{
		transactionCommiter.run();
	}
	
	public void resetTransaction()
	{
		resetHandler.run();
	}
}