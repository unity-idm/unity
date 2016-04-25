/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

/**
 * CRUD interface used by most tables which have unique String name key.
 * @author K. Benedyczak
 * @param <BEAN>
 */
public interface NamedCRUDMapper<BEAN> extends BasicCRUDMapper<BEAN>
{
	void delete(String id);
	
	BEAN getByName(String id);
}
