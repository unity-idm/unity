/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.tx.TxManager;

/**
 * Delegates public API of {@link TxManager} to internal implementation based on thread locals. 
 * @author K. Benedyczak
 */
@Component
public class TxManagerImpl implements TxManager
{
	@Override
	public void commit()
	{
		TransactionTL.manualCommit();
	}
}
