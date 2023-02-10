/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler.PlannedUpdateEvent;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.exceptions.EntityNotFoundException;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base implementation of RDBMS based CRUD DAO.
 * @author K. Benedyczak
 */
public abstract class GenericRDBMSCRUD<T, DBT extends GenericDBBean> 
		implements BasicCRUDDAO<T>, RDBMSDAO, ReferenceAwareDAO<T>
{
	private final Class<? extends BasicCRUDMapper<DBT>> mapperClass;
	protected final RDBMSObjectSerializer<T, DBT> jsonSerializer;
	protected final String elementName;
	private final Set<ReferenceRemovalHandler> deleteHandlers = new HashSet<>();
	private final Set<ReferenceUpdateHandler<T>> updateHandlers = new HashSet<>();
	private final Function<Long, RuntimeException> missingElementExceptionProvider;

	public GenericRDBMSCRUD(Class<? extends BasicCRUDMapper<DBT>> mapperClass,
			RDBMSObjectSerializer<T, DBT> jsonSerializer, String elementName)
	{
		this(mapperClass, jsonSerializer, elementName, 
				id -> new EntityNotFoundException(elementName + " with key [" + id +
						"] does not exist"));
	}
	
	public GenericRDBMSCRUD(Class<? extends BasicCRUDMapper<DBT>> mapperClass,
			RDBMSObjectSerializer<T, DBT> jsonSerializer, String elementName,
			Function<Long, RuntimeException> missingElementExceptionProvider)
	{
		this.mapperClass = mapperClass;
		this.jsonSerializer = jsonSerializer;
		this.elementName = elementName;
		this.missingElementExceptionProvider = missingElementExceptionProvider;
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
	public List<Long> createList(List<T> objs)
	{
		if (objs.isEmpty())
			return Collections.emptyList();
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		List<DBT> converted = new ArrayList<>(objs.size());
		for (T obj: objs)
		{
			DBT toAdd = jsonSerializer.toDB(obj);
			assertContentsLimit(toAdd.getContents());
			converted.add(toAdd);
		}
		mapper.createList(converted);
		
		return converted.stream()
				.map(GenericDBBean::getId)
				.collect(Collectors.toList());
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
		DBT oldBean = mapper.getByKey(key);
		if (oldBean == null)
			throw missingElementExceptionProvider.apply(key);
		T old = jsonSerializer.fromDB(oldBean);
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
	 */
	protected void preUpdateCheck(T old, T updated)
	{
	}
	
	@Override
	public void deleteByKey(long id)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT toRemove = mapper.getByKey(id);
		if (toRemove == null)
			throw missingElementExceptionProvider.apply(id);
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
			throw missingElementExceptionProvider.apply(id);
	}

	@Override
	public T getByKey(long id)
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		DBT byName = mapper.getByKey(id);
		if (byName == null)
			throw missingElementExceptionProvider.apply(id);
		return jsonSerializer.fromDB(byName);
	}

	@Override
	public List<T> getAll()
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		List<DBT> allInDB = mapper.getAll();
		return convertList(allInDB);
	}
	
	@Override
	public long getCount()
	{
		BasicCRUDMapper<DBT> mapper = SQLTransactionTL.getSql().getMapper(mapperClass);
		return mapper.getCount();
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

	protected void firePreUpdate(long modifiedId, String modifiedName, T newVal, T old)
	{
		for (ReferenceUpdateHandler<T> handler: updateHandlers)
			handler.preUpdateCheck(new PlannedUpdateEvent<>(modifiedId, modifiedName, newVal, old));
	}
}
