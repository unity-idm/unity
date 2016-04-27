/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * Base implementation of RDBMS based CRUD DAO.
 * @author K. Benedyczak
 */
public abstract class GenericRDBMSCRUD<T, DBT extends BaseBean> implements BasicCRUDDAO<T>, RDBMSDAO
{
	private Class<? extends BasicCRUDMapper<DBT>> mapperClass;
	protected final RDBMSObjectSerializer<T, DBT> jsonSerializer;
	protected final String elementName;
	protected final StorageLimits limits;
	
	public GenericRDBMSCRUD(Class<? extends BasicCRUDMapper<DBT>> mapperClass,
			RDBMSObjectSerializer<T, DBT> jsonSerializer, String elementName,
			StorageLimits limits)
	{
		this.mapperClass = mapperClass;
		this.jsonSerializer = jsonSerializer;
		this.elementName = elementName;
		this.limits = limits;
	}

	@Override
	public long create(T obj)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT toAdd = jsonSerializer.toDB(obj);
		limits.checkContentsLimit(toAdd.getContents());
		mapper.create(toAdd);
		return toAdd.getId();
	}

	@Override
	public void updateByKey(long key, T obj)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		assertExists(key, mapper);
		DBT toUpdate = jsonSerializer.toDB(obj);
		limits.checkContentsLimit(toUpdate.getContents());
		toUpdate.setId(key);
		mapper.updateByKey(toUpdate);		
	}

	@Override
	public void deleteByKey(long id)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		assertExists(id, mapper);
		mapper.deleteByKey(id);
	}

	protected void assertExists(long id, BasicCRUDMapper<DBT> mapper)
	{
		if (mapper.getByKey(id) == null)
			throw new IllegalArgumentException(elementName + " with key [" + id + 
					"] does not exist");
	}

	@Override
	public T getByKey(long id)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT byName = mapper.getByKey(id);
		if (byName == null)
			throw new IllegalArgumentException(elementName + " with key [" + id + 
					"] does not exist");
		return jsonSerializer.fromDB(byName);
	}

	@Override
	public List<T> getAll()
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		List<DBT> allInDB = mapper.getAll();
		List<T> ret = new ArrayList<>(allInDB.size());
		for (DBT bean: allInDB)
		{
			T obj = jsonSerializer.fromDB(bean);
			ret.add(obj);
		}
		return ret;
	}
}
