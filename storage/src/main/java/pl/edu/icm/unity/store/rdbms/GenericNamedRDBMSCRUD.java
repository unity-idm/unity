/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.exceptions.PersistenceException;

import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.exceptions.EntityNotFoundException;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.UpdateFlag;

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
			RDBMSObjectSerializer<T, DBT> jsonSerializer, String elementName)
	{
		super(namedMapperClass, jsonSerializer, elementName);
		this.namedMapperClass = namedMapperClass;
	}

	@Override
	public long create(T obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
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
	public void updateByKey(long key, T obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
		super.updateByKey(key, obj);
	}

	@Override
	public void updateByNameControlled(String current, T obj, EnumSet<UpdateFlag> updateFlags)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		DBT byName = mapper.getByName(current);
		if (byName == null)
			throw new IllegalArgumentException(elementName + " [" + current + 
					"] does not exist");
		updateByKey(byName.getId(), obj);
	}

	@Override
	public void delete(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		DBT toRemove = mapper.getByName(id);
		if (toRemove == null)
			throw new EntityNotFoundException(elementName + " [" + id +
					"] does not exist");
		firePreRemove(toRemove.getId(), id, toRemove);
		mapper.delete(id);
	}
	
	@Override
	public T get(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		DBT byName = mapper.getByName(id);
		if (byName == null)
			throw new EntityNotFoundException(elementName + " [" + id +
					"] does not exist");
		return jsonSerializer.fromDB(byName);
	}
	
	@Override
	public long getKeyForName(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		DBT byName = mapper.getByName(id);
		if (byName == null)
			throw new EntityNotFoundException(elementName + " [" + id +
					"] does not exist");
		return byName.getId();
	}
	
	@Override
	public boolean exists(String id)
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		return mapper.getByName(id) != null;
	}

	@Override
	public Map<String, T> getAllAsMap()
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

	@Override
	public Set<String> getAllNames()
	{
		NamedCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(namedMapperClass);
		return mapper.getAllNames();
	}
	
	@Override
	protected void firePreRemove(long modifiedId, String modifiedName, DBT old)
	{
		super.firePreRemove(modifiedId, old.getName(), old);
	}

	@Override
	protected void firePreUpdate(long modifiedId, String modifiedName, T newVal, T old)
	{
		super.firePreUpdate(modifiedId, old.getName(), newVal, old);
	}
}
