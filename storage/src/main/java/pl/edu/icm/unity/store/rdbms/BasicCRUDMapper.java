/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.List;

/**
 * All mappers extend this interface, and those operations are available on all tables.
 * @author K. Benedyczak
 * @param <BEAN>
 */
public interface BasicCRUDMapper<BEAN>
{
	void create(BEAN obj);
	
	void update(BEAN obj);

	void delete(String id);

	BEAN getByName(String id);

	List<BEAN> getAll();
	
	BEAN getByKey(long key);
}
