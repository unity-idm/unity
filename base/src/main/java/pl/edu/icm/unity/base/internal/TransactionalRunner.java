/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.internal;

public interface TransactionalRunner
{
	public void runInTransaction(TxRunnable code);
	
	public <T> T runInTransactionRet(TxRunnableRet<T> code);
	
	public interface TxRunnable
	{
		void run();
	}

	public interface TxRunnableRet<T>
	{
		T run();
	}
}
