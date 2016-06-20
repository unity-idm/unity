/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

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
	 * Should be called after calling {@link #deleteEverything()} method and subsequently adding new data:
	 * performs additional cleanup needed for some DBs (currently only PSQL).
	 */
	void runPostImportCleanup();
}
