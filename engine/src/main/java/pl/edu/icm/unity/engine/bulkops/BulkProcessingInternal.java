/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Group bulk processing actions which are used internally and not exposed with the engine API.
 * @author K. Benedyczak
 */
public interface BulkProcessingInternal
{
	/**
	 * Undeploys all scheduled rules, and removes them from database.
	 * 
	 * @throws EngineException
	 */
	void removeAllRules() throws EngineException;
}
