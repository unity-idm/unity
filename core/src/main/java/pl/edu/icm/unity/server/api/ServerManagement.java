/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to general maintenance operations.
 * @author K. Benedyczak
 */
public interface ServerManagement
{
	/**
	 * Removes the whole contents of the database and initializes it from scratch.
	 * @throws EngineException
	 */
	public void resetDatabase() throws EngineException;
}
