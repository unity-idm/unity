/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Trivial cache hashmap based, no automatic flushing
 * 
 * @author K. Benedyczak
 */
public class HashMapBasicCache<T> implements BasicCache<T>
{
	protected boolean cacheComplete; 
	protected Map<Long, T> byKey = new HashMap<>();
	protected List<T> all = new ArrayList<>();
	protected final Function<T, T> cloner;
	
	public HashMapBasicCache(Function<T, T> cloner)
	{
		this.cloner = new NullSafeCloner<>(cloner);
	}

	@Override
	public synchronized void flush()
	{
		cacheComplete = false;
		byKey.clear();
	}

	@Override
	public synchronized void storeById(long id, T element)
	{
		byKey.put(id, element);
	}

	@Override
	public synchronized void storeAll(List<T> elements)
	{
		cacheComplete = true;
		all.clear();
		for (T element: elements)
			all.add(cloner.apply(element));
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
		
		List<T> ret = new ArrayList<T>(all.size());
		for (T val: all)
			ret.add(cloner.apply(val));
		return Optional.of(ret);
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
