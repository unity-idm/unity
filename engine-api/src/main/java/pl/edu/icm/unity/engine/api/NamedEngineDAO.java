/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * DAO with typical engine operations for types which are identifiable by string
 * name.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface NamedEngineDAO<T> extends BasicEngineDAO<T>
{
	void updateByName(String name, T newValue) throws EngineException;
	
	void deleteByName(String name) throws EngineException;
	
	boolean exists(String id) throws EngineException;

	T get(String id) throws EngineException;
}
