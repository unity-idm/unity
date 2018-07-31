/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;

/**
 * Enables caching of the operations with cache flush on every write. 
 *  
 * @author K. Benedyczak
 */
public class BasicCachingCRUD<T, DAO extends BasicCRUDDAO<T>, CACHE extends BasicCache<T>> implements BasicCRUDDAO<T>
{
	protected final DAO wrapped;
	protected final CACHE cache;
	
	public BasicCachingCRUD(DAO wrapped, CACHE cache)
	{
		this.wrapped = wrapped;
		this.cache = cache;
	}

	@Override
	public long create(T obj)
	{
		cache.flush();
		return wrapped.create(obj);
	}

	@Override
	public void createWithId(long id, T obj)
	{
		cache.flush();
		wrapped.createWithId(id, obj);
	}

	@Override
	public void updateByKey(long id, T obj)
	{
		cache.flush();
		wrapped.updateByKey(id, obj);
	}

	@Override
	public void deleteByKey(long id)
	{
		cache.flush();
		wrapped.deleteByKey(id);
	}

	@Override
	public void deleteAll()
	{
		cache.flush();
		wrapped.deleteAll();
	}

	@Override
	public T getByKey(long id)
	{
		Optional<T> cached = cache.getByKey(id);
		if (cached.isPresent())
			return cached.get();
		T element = wrapped.getByKey(id);
		cache.storeById(id, element);
		return element;
	}

	@Override
	public List<T> getAll()
	{
		Optional<List<T>> cached = cache.getAll();
		if (cached.isPresent())
			return cached.get();
		List<T> elements = wrapped.getAll();
		cache.storeAll(elements);
		return elements;
	}
}
