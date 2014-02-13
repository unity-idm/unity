/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.io.File;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to general maintenance operations.
 * @author K. Benedyczak
 */
public interface ServerManagement
{
	public static final String DB_DUMP_DIRECTORY = "databaseDumps";
	public static final String DB_IMPORT_DIRECTORY = "databaseUploads";
	
	/**
	 * Removes the whole contents of the database and initializes it from scratch.
	 * @throws EngineException
	 */
	public void resetDatabase() throws EngineException;
	
	/**
	 * Exports the whole database contents to a JSON file.
	 * @return the file reference
	 * @throws EngineException
	 */
	public File exportDb() throws EngineException;
	
	/**
	 * Imports the whole database from a given JSON file
	 * @param from file to load data from
	 * @throws EngineException
	 */
	public void importDb(File from, boolean resetIndexes) throws EngineException;
	
	/**
	 * Reload configuration file if changed
	 * @throws EngineException
	 */
	public void reloadConfig() throws EngineException;
}
