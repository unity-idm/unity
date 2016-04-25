/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Map;

import pl.edu.icm.unity.types.NamedObject;

/**
 * DAO with typical CRUD operations for types which are identifieable by string name.
 * @author K. Benedyczak
 */
public interface NamedCRUDDAO<T extends NamedObject> extends BasicCRUDDAO<T>
{	
	T get(String id);
	
	void delete(String id);
	
	void update(T obj);
	
	boolean exists(String id);
	
	Map<String, T> getAsMap();
	
}
