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
public interface BasicEngineDAO<T>
{
	void create(T obj) throws EngineException;
	
	void update(T obj) throws EngineException;
	
	void delete(T obj) throws EngineException;
	
	List<T> getAll() throws EngineException;
}
