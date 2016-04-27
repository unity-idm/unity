/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.exceptions.PersistenceException;

import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Base implementation of RDBMS based CRUD DAO of named objects.
 * @author K. Benedyczak
 */
public abstract class GenericNamedRDBMSCRUD<T extends NamedObject, DBT extends BaseBean> 
		extends GenericRDBMSCRUD<T, DBT> 
		implements NamedCRUDDAO<T>, RDBMSDAO
{
	private Class<? extends NamedCRUDMapper<DBT>> namedMapperClass;
	
	public static final String SQL_DUP_1_ERROR = "23000";
	public static final String SQL_DUP_2_ERROR = "23505";
	
	public GenericNamedRDBMSCRUD(Class<? extends NamedCRUDMapper<DBT>> namedMapperClass,
			RDBMSObjectSerializer<T, DBT> jsonSerializer, String elementName, StorageLimits limits)
	{
		super(namedMapperClass, jsonSerializer, elementName, limits);
		this.namedMapperClass = namedMapperClass;
	}

	@Override
	public long create(T obj)
	{
		limits.checkNameLimit(obj.getName());
		try
		{
			return super.create(obj);
		} catch (PersistenceException e)
		{
			Throwable causeO = e.getCause();
			if (!(causeO instanceof SQLException))
				throw e;
			SQLException cause = (SQLException) causeO;
			if (cause.getSQLState().equals(SQL_DUP_1_ERROR) || 
					cause.getSQLState().equals(SQL_DUP_2_ERROR))
				throw new IllegalArgumentException(elementName + " [" + obj.getName() + 
						"] already exist", e);
			throw e;
		}
	}

	@Override
	public void update(T obj)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		DBT byName = mapper.getByName(obj.getName());
		if (byName == null)
			throw new IllegalArgumentException(elementName + " [" + obj.getName() + 
					"] does not exist");
		DBT toUpdate = jsonSerializer.toDB(obj);
		toUpdate.setId(byName.getId());
		mapper.updateByKey(toUpdate);		
	}

	@Override
	public void updateByKey(long key, T obj)
	{
		limits.checkNameLimit(obj.getName());
		super.updateByKey(key, obj);
	}
	
	@Override
	public void delete(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		assertExists(id, mapper);
		mapper.delete(id);
	}

	@Override
	public T get(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		DBT byName = mapper.getByName(id);
		if (byName == null)
			throw new IllegalArgumentException(elementName + " [" + id + 
					"] does not exist");
		return jsonSerializer.fromDB(byName);
	}

	@Override
	public boolean exists(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		return mapper.getByName(id) != null;
	}

	@Override
	public Map<String, T> getAsMap()
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		List<DBT> allInDB = mapper.getAll();
		Map<String, T> ret = new HashMap<>(allInDB.size());
		for (DBT bean: allInDB)
		{
			T obj = jsonSerializer.fromDB(bean);
			ret.put(obj.getName(), obj);
		}
		return ret;
	}

	private void assertExists(String id, NamedCRUDMapper<DBT> mapper)
	{
		if (!exists(id, mapper))
			throw new IllegalArgumentException(elementName + " [" + id + 
					"] does not exist");
	}
	
	private boolean exists(String id, NamedCRUDMapper<DBT> mapper)
	{
		return mapper.getByName(id) != null;
	}
}
