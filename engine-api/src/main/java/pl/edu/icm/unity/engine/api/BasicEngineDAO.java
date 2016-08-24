/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

/**
 * Basic DAO with typical engine operations.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface BasicEngineDAO<T>
{
	long create(T obj);
	
	void update(T obj);
	
	List<T> getAll();
}
