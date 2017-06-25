/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import pl.edu.icm.unity.store.api.StorageCleaner;

/**
 * For storage engines: initialization and cleanup of storage.
 * This interface is delegate of {@link StorageCleaner} public API, implemented by individual 
 * storage engines.
 * @author K. Benedyczak
 */
public interface StoreLoaderInternal
{
	/**
	 * Database tables are dropped and database is recreated. This is the most complete 
	 * reset of the database.
	 */
	void reset();
	
	/**
	 * Similar in effect to {@link #reset()} however transaction-friendly. Everything in database is 
	 * removed, besides (maybe) some fundamental meta tables. The root group is created afterwards.
	 */
	void deleteEverything();
	
	
	void runPostImportCleanup();
	
	/**
	 * Stops and cleanups the store
	 */
	void shutdown();
}
