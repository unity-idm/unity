/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.util.Map;

import pl.edu.icm.unity.store.api.CRUDDAO;

/**
 * Implementation of CRUD DAO using underlying RDBMS and Hazelcast DAOs. Hazelcast is used for reading,
 * both are used for writing.
 *  
 * @author K. Benedyczak
 */
public class GenericCompositeDAOImpl<T> implements CRUDDAO<T>
{
	private CRUDDAO<T> hzDAO;
	private CRUDDAO<T> rdbmsDAO;
	
	public GenericCompositeDAOImpl(CRUDDAO<T> hzDAO, CRUDDAO<T> rdbmsDAO)
	{
		this.hzDAO = hzDAO;
		this.rdbmsDAO = rdbmsDAO;
	}

	@Override
	public void create(T obj)
	{
		rdbmsDAO.create(obj);
		hzDAO.create(obj);
	}

	@Override
	public void update(T obj)
	{
		rdbmsDAO.update(obj);
		hzDAO.update(obj);
	}

	@Override
	public void delete(String id)
	{
		rdbmsDAO.delete(id);
		hzDAO.delete(id);
	}

	@Override
	public T get(String id)
	{
		return hzDAO.get(id);
	}

	@Override
	public Map<String, T> getAsMap()
	{
		return hzDAO.getAsMap();
	}

	@Override
	public boolean exists(String id)
	{
		return hzDAO.exists(id);
	}
}
