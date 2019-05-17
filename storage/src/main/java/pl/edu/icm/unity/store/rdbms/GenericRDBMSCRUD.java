/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * Base implementation of RDBMS based CRUD DAO.
 * @author K. Benedyczak
 */
public abstract class GenericRDBMSCRUD<T, DBT extends GenericDBBean> 
		implements BasicCRUDDAO<T>, RDBMSDAO, ReferenceAwareDAO<T>
{
	private Class<? extends BasicCRUDMapper<DBT>> mapperClass;
	protected final RDBMSObjectSerializer<T, DBT> jsonSerializer;
	protected final String elementName;
	private Set<ReferenceRemovalHandler> deleteHandlers = new HashSet<>();
	private Set<ReferenceUpdateHandler<T>> updateHandlers = new HashSet<>();
	
	public GenericRDBMSCRUD(Class<? extends BasicCRUDMapper<DBT>> mapperClass,
			RDBMSObjectSerializer<T, DBT> jsonSerializer, String elementName)
	{
		this.mapperClass = mapperClass;
		this.jsonSerializer = jsonSerializer;
		this.elementName = elementName;
	}

	@Override
	public long create(T obj)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT toAdd = jsonSerializer.toDB(obj);
		assertContentsLimit(toAdd.getContents());
		mapper.create(toAdd);
		return toAdd.getId();
	}

	@Override
	public void createWithId(long key, T obj)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT toAdd = jsonSerializer.toDB(obj);
		toAdd.setId(key);
		assertContentsLimit(toAdd.getContents());
		mapper.createWithKey(toAdd);
	}
	
	@Override
	public void updateByKey(long key, T obj)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT old = mapper.getByKey(key);
		if (old == null)
			throw new IllegalArgumentException(elementName + " with key [" + key + 
					"] does not exist");
		preUpdateCheck(old, obj);
		firePreUpdate(key, null, obj, old);
		DBT toUpdate = jsonSerializer.toDB(obj);
		assertContentsLimit(toUpdate.getContents());
		toUpdate.setId(key);
		mapper.updateByKey(toUpdate);		
	}

	protected void assertContentsLimit(byte[] contents)
	{
		StorageLimits.checkContentsLimit(contents);
	}
	
	/**
	 * For extensions
	 * @param old
	 * @param updated
	 */
	protected void preUpdateCheck(DBT old, T updated)
	{
	}
	
	@Override
	public void deleteByKey(long id)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT toRemove = mapper.getByKey(id);
		if (toRemove == null)
			throw new IllegalArgumentException(elementName + " with key [" + id + 
					"] does not exist");
		firePreRemove(id, null, toRemove);
		mapper.deleteByKey(id);
	}
	
	@Override
	public void deleteAll()
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		mapper.deleteAll();
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
		return convertList(allInDB);
	}
	
	protected List<T> convertList(List<DBT> fromDB)
	{
		List<T> ret = new ArrayList<>(fromDB.size());
		for (DBT bean: fromDB)
		{
			T obj = jsonSerializer.fromDB(bean);
			ret.add(obj);
		}
		return ret;
	}
	
	@Override
	public void addRemovalHandler(ReferenceRemovalHandler handler)
	{
		deleteHandlers.add(handler);
	}
	
	@Override
	public void addUpdateHandler(ReferenceUpdateHandler<T> handler)
	{
		updateHandlers.add(handler);
	}

	protected void firePreRemove(long modifiedId, String modifiedName, DBT old)
	{
		for (ReferenceRemovalHandler handler: deleteHandlers)
			handler.preRemoveCheck(modifiedId, modifiedName);
	}

	protected void firePreUpdate(long modifiedId, String modifiedName, T newVal, DBT old)
	{
		for (ReferenceUpdateHandler<T> handler: updateHandlers)
			handler.preUpdateCheck(modifiedId, modifiedName, newVal);
	}
}
