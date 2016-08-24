/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

/**
 * DAO with typical engine operations for types which are identifiable by string
 * name.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface NamedEngineDAO<T> extends BasicEngineDAO<T>
{
	void updateByName(String current, T newValue);
	
	boolean exists(String id);

	T get(String id);
}
