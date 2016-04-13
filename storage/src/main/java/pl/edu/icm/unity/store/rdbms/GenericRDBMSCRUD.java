/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.tx.TransactionTL;

/**
 * Base implementation of RDBMS based CRUD DAO.
 * @author K. Benedyczak
 */
public abstract class GenericRDBMSCRUD<T, DBT> implements BasicCRUDDAO<T> 
{
	private Class<? extends BasicCRUDMapper<DBT>> mapperClass;
	private RDBMSObjectSerializer<T, DBT> jsonSerializer;
	private String elementName;
	private DBLimit limits;
	
	public GenericRDBMSCRUD(Class<? extends BasicCRUDMapper<DBT>> mapperClass,
			RDBMSObjectSerializer<T, DBT> jsonSerializer, DBLimit limits, String elementName)
	{
		this.mapperClass = mapperClass;
		this.jsonSerializer = jsonSerializer;
		this.limits = limits;
		this.elementName = elementName;
	}

	protected abstract String getNameId(T obj);
	
	@Override
	public void create(T obj)
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		limits.checkNameLimit(getNameId(obj));
		assertDoesNotExist(obj, mapper);
		DBT toAdd = jsonSerializer.toDB(obj);
		mapper.create(toAdd);
	}

	@Override
	public void update(T obj)
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		limits.checkNameLimit(getNameId(obj));
		assertExists(obj, mapper);
		DBT toUpdate = jsonSerializer.toDB(obj);
		mapper.update(toUpdate);		
	}

	@Override
	public void delete(String id)
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		assertExists(id, mapper);
		mapper.delete(id);
	}

	@Override
	public T get(String id)
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		DBT byName = mapper.getByName(id);
		if (byName == null)
			throw new IllegalArgumentException(elementName + " [" + id + 
					"] does not exist");
		return jsonSerializer.fromDB(byName);
	}

	@Override
	public boolean exists(String id)
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		return mapper.getByName(id) != null;
	}

	@Override
	public Map<String, T> getAsMap()
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		List<DBT> allInDB = mapper.getAll();
		Map<String, T> ret = new HashMap<>(allInDB.size());
		for (DBT bean: allInDB)
		{
			T obj = jsonSerializer.fromDB(bean);
			ret.put(getNameId(obj), obj);
		}
		return ret;
	}

	@Override
	public List<T> getAll()
	{
		BasicCRUDMapper<DBT> mapper = TransactionTL.getSql().getMapper(mapperClass);
		List<DBT> allInDB = mapper.getAll();
		List<T> ret = new ArrayList<>(allInDB.size());
		for (DBT bean: allInDB)
		{
			T obj = jsonSerializer.fromDB(bean);
			ret.add(obj);
		}
		return ret;
	}
	
	private void assertExists(T obj, BasicCRUDMapper<DBT> mapper)
	{
		assertExists(getNameId(obj), mapper);
	}
	
	private void assertExists(String id, BasicCRUDMapper<DBT> mapper)
	{
		if (!exists(id, mapper))
			throw new IllegalArgumentException(elementName + " [" + id + 
					"] does not exist");
	}
	
	private void assertDoesNotExist(T obj, BasicCRUDMapper<DBT> mapper)
	{
		String id = getNameId(obj);
		if (exists(id, mapper))
			throw new IllegalArgumentException(elementName + " [" + id + 
					"] already exists");
	}
	
	private boolean exists(String id, BasicCRUDMapper<DBT> mapper)
	{
		return mapper.getByName(id) != null;
	}
}
