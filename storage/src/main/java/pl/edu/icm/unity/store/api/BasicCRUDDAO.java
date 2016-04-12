/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

/**
 * Basic DAO with typical CRUD operations
 * @author K. Benedyczak
 */
public interface BasicCRUDDAO<T>
{	
	void create(T obj);
	
	void update(T obj);

	void delete(String id);

	T get(String id);
	
	boolean exists(String id);
}
