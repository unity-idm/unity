/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.cache.Cache;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Trivial cache hashmap based, no automatic flushing
 * 
 * @author K. Benedyczak
 */
public class HashMapNamedCache<T extends NamedObject> extends GuavaBasicCache<T> implements NamedCache<T>
{
	private Cache<String, T> byName;
	private boolean cacheComplete;
	
	public HashMapNamedCache(Function<T, T> cloner)
	{
		super(cloner);
	}

	@Override
	public synchronized void configure(int ttl, int max)
	{
		super.configure(ttl, max);
		if (disabled)
			return;
		byName = getBuilder(ttl, max).build();
	}
	
	@Override
	public synchronized void flushWithoutEvent()
	{
		if (disabled) 
			return;
		super.flushWithoutEvent();
		cacheComplete = false;
		byName.invalidateAll();
	}

	@Override
	public synchronized void storeById(long id, T element)
	{
		if (disabled) 
			return;
		super.storeById(id, element);
		byName.put(element.getName(), cloner.apply(element));
	}

	@Override
	public synchronized void storeByName(String id, T element)
	{
		if (disabled) 
			return;
		byName.put(id, cloner.apply(element));
	}

	@Override
	public synchronized void storeAll(List<T> elements)
	{
		if (disabled) 
			return;
		super.storeAll(elements);
		byName.invalidateAll();
		for (T element: elements)
			byName.put(element.getName(), cloner.apply(element));
		cacheComplete = true;
	}



	@Override
	public synchronized Optional<Boolean> exists(String id)
	{
		if (disabled) 
			return Optional.empty();
		if (!cacheComplete)
			return Optional.empty();
		return Optional.of(byName.asMap().containsKey(id));
	}

	@Override
	public synchronized Optional<Map<String, T>> getAllAsMap()
	{
		if (disabled) 
			return Optional.empty();
		if (!cacheComplete)
			return Optional.empty();
		
		Map<String, T> ret = new HashMap<>();
		for (Map.Entry<String, T> entry: byName.asMap().entrySet())
			ret.put(entry.getKey(), cloner.apply(entry.getValue()));
		return Optional.of(ret);
	}

	@Override
	public synchronized Optional<T> get(String id)
	{
		if (disabled) 
			return Optional.empty();
		return Optional.ofNullable(cloner.apply(byName.getIfPresent(id)));
	}

	@Override
	public synchronized Optional<Long> getKeyForName(String id)
	{
		if (disabled) 
			return Optional.empty();
		return Optional.empty();
	}

	@Override
	public synchronized Optional<Set<String>> getAllNames()
	{
		if (disabled) 
			return Optional.empty();
		if (!cacheComplete)
			return Optional.empty();
		return Optional.of(new HashSet<>(byName.asMap().keySet()));
	}
}
