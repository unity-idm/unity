/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Extension of {@link BasicCachingCRUD}, adds handling of caching of operations of {@link NamedCRUDDAO}.
 * 
 * @author K. Benedyczak
 * @param <T>
 * @param <DAO>
 * @param <CACHE>
 */
public class NamedCachingCRUD<T extends NamedObject, DAO extends NamedCRUDDAO<T>, CACHE extends NamedCache<T>> 
		extends BasicCachingCRUD<T, DAO, CACHE> implements NamedCRUDDAO<T>
{
	public NamedCachingCRUD(DAO wrapped, CACHE cache)
	{
		super(wrapped, cache);
	}

	@Override
	public void delete(String id)
	{
		cache.flushWithEvent();
		wrapped.delete(id);
	}

	@Override
	public void updateByName(String current, T newValue)
	{
		cache.flushWithEvent();
		wrapped.updateByName(current, newValue);
	}

	@Override
	public boolean exists(String id)
	{
		Optional<Boolean> cached = cache.exists(id);
		if (cached.isPresent())
			return cached.get();
		return wrapped.exists(id);
	}

	@Override
	public Map<String, T> getAllAsMap()
	{
		Optional<Map<String, T>> cached = cache.getAllAsMap();
		if (cached.isPresent())
			return cached.get();
		
		Map<String, T> elements = wrapped.getAllAsMap();
		cache.storeAll(new ArrayList<>(elements.values()));
		return elements;
	}

	@Override
	public T get(String id)
	{
		Optional<T> cached = cache.get(id);
		if (cached.isPresent())
			return cached.get();
		
		T element = wrapped.get(id);
		cache.storeByName(id, element);
		return element;
	}

	@Override
	public long getKeyForName(String id)
	{
		Optional<Long> cached = cache.getKeyForName(id);
		if (cached.isPresent())
			return cached.get();
		return wrapped.getKeyForName(id);
	}

	@Override
	public Set<String> getAllNames()
	{
		Optional<Set<String>> cached = cache.getAllNames();
		if (cached.isPresent())
			return cached.get();
		return wrapped.getAllNames();
	}

}
