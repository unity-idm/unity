/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Basic DAO with typical engine operations.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface BasicEngineDAO<PARAM, ENTITY>
{
	void create(PARAM obj) throws EngineException;
	
	void update(PARAM obj) throws EngineException;
	
	void delete(PARAM obj) throws EngineException;
	
	List<ENTITY> getAll() throws EngineException;
}
