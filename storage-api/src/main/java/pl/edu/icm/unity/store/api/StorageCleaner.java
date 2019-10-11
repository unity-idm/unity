/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

/**
 * Storage engine independent storage cleaning.
 * 
 * @author K. Benedyczak
 */
public interface StorageCleaner
{
	/**
	 * Performs a hard reset of the database. Everything is dropped, and the schema is recreated.
	 */
	void reset();

	/**
	 * Deletes everything from the database but leaves schema untouched. 
	 */
	void deleteEverything();


	/**
	 * Deletes data selected to import from database. 
	 */
	void deletePreImport(List<String> dbContent);

	
	/**
	 * Should be called after calling {@link #deleteEverything()} method and subsequently adding new data:
	 * performs additional cleanup needed for some DBs (currently only PSQL).
	 */
	void runPostImportCleanup();
	
	/**
	 * Manual shutdown of the store
	 */
	void shutdown();

	/**
	 * On first time in JVM {@link #reset()} is called, on subsequent runs {@link #deleteEverything()} is invoked
	 */
	void cleanOrDelete();
}
