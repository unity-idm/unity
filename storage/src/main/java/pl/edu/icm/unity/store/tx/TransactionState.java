/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

/**
 * Implemented by all kinds of transactions.
 * @author K. Benedyczak
 */
public interface TransactionState
{
	void manualCommit();
}
