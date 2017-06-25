/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.tx;

/**
 * Allows for manual manipulation of transaction state 
 * @author K. Benedyczak
 */
public interface TxManager
{
	/**
	 * Performs manual commit. Useful in code which is running in a transaction with auto commit turned off.
	 */
	void commit();
}
