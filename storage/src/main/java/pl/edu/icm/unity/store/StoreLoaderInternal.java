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
	void reset();
	void deleteEverything();
	void runPostImportCleanup();
}
