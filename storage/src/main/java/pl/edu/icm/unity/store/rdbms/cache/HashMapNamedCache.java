/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Trivial cache hashmap based, no automatic flushing
 * 
 * @author K. Benedyczak
 */
public class HashMapNamedCache<T extends NamedObject> implements NamedCache<T>
{
	private boolean cacheComplete; 
	private Map<String, T> byName = new HashMap<>();
	private Map<Long, T> byKey = new HashMap<>();
	protected final Function<T, T> cloner;
	
	public HashMapNamedCache(Function<T, T> cloner)
	{
		this.cloner = new NullSafeCloner<>(cloner);
	}

	@Override
	public synchronized void flush()
	{
		cacheComplete = false;
		byName.clear();
		byKey.clear();
	}

	@Override
	public synchronized void storeById(long id, T element)
	{
		byName.put(element.getName(), cloner.apply(element));
		byKey.put(id, element);
	}

	@Override
	public synchronized void storeByName(String id, T element)
	{
		byName.put(id, cloner.apply(element));
	}

	@Override
	public synchronized void storeAll(List<T> elements)
	{
		cacheComplete = true;
		byName.clear();
		for (T element: elements)
			byName.put(element.getName(), cloner.apply(element));
	}



	@Override
	public synchronized Optional<T> getByKey(long id)
	{
		return Optional.ofNullable(cloner.apply(byKey.get(id)));
	}

	@Override
	public synchronized Optional<List<T>> getAll()
	{
		if (!cacheComplete)
			return Optional.empty();
		
		List<T> ret = new ArrayList<T>(byName.size());
		for (T val: byName.values())
			ret.add(cloner.apply(val));
		return Optional.of(ret);
	}

	@Override
	public synchronized Optional<Boolean> exists(String id)
	{
		if (!cacheComplete)
			return Optional.empty();
		return Optional.of(byName.containsKey(id));
	}

	@Override
	public synchronized Optional<Map<String, T>> getAllAsMap()
	{
		if (!cacheComplete)
			return Optional.empty();
		
		Map<String, T> ret = new HashMap<>();
		for (Map.Entry<String, T> entry: byName.entrySet())
			ret.put(entry.getKey(), cloner.apply(entry.getValue()));
		return Optional.of(ret);
	}

	@Override
	public synchronized Optional<T> get(String id)
	{
		return Optional.ofNullable(cloner.apply(byName.get(id)));
	}

	@Override
	public synchronized Optional<Long> getKeyForName(String id)
	{
		return Optional.empty();
	}

	@Override
	public synchronized Optional<Set<String>> getAllNames()
	{
		if (!cacheComplete)
			return Optional.empty();
		return Optional.of(new HashSet<>(byName.keySet()));
	}

	private static class NullSafeCloner<T> implements Function<T, T>
	{
		private final Function<T, T> cloner;

		public NullSafeCloner(Function<T, T> cloner)
		{
			this.cloner = cloner;
		}

		@Override
		public T apply(T t)
		{
			return t == null ? null : cloner.apply(t);
		}
	}
}
