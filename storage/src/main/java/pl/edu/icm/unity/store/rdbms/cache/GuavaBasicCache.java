/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Trivial cache hashmap based, no automatic flushing
 * 
 * @author K. Benedyczak
 */
public class GuavaBasicCache<T> implements BasicCache<T>
{
	protected final Integer SINGLE_ENTRY_KEY = 1;
	protected boolean disabled = true;
	protected Cache<Long, T> byKey;
	protected Cache<Integer, List<T>> all;
	protected final Function<T, T> cloner;
	
	public GuavaBasicCache(Function<T, T> cloner)
	{
		this.cloner = new NullSafeCloner<>(cloner);
	}

	@Override
	public synchronized void configure(int ttl, int max)
	{
		if (ttl == 0 || max == 0)
		{
			disabled = true;
			return;
		}
		disabled = false;
		byKey = getBuilder(ttl, max).build();
		all = getBuilder(ttl, max).build();
	}
	
	protected CacheBuilder<Object, Object> getBuilder(int ttl, int max)
	{
		CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
		if (ttl > 0)
			builder.expireAfterWrite(ttl, TimeUnit.SECONDS);
		if (max > 0)
			builder.maximumSize(max);
		return builder;
	}
	
	@Override
	public synchronized void flush()
	{
		if (disabled) 
			return;
		byKey.invalidateAll();
		all.invalidateAll();
	}

	@Override
	public synchronized void storeById(long id, T element)
	{
		if (disabled) 
			return;
		byKey.put(id, element);
	}

	@Override
	public synchronized void storeAll(List<T> elements)
	{
		if (disabled) 
			return;
		all.invalidateAll();
		List<T> allL = new ArrayList<>(elements.size());
		for (T element: elements)
			allL.add(cloner.apply(element));
		all.put(SINGLE_ENTRY_KEY, allL);
	}

	@Override
	public synchronized Optional<T> getByKey(long id)
	{
		if (disabled) 
			return Optional.empty();
		return Optional.ofNullable(cloner.apply(byKey.getIfPresent(id)));
	}

	@Override
	public synchronized Optional<List<T>> getAll()
	{
		if (disabled) 
			return Optional.empty();
		List<T> allL = all.getIfPresent(SINGLE_ENTRY_KEY);
		if (allL == null)
			return Optional.empty();
		return Optional.of(cloneList(allL));
	}

	protected List<T> cloneList(List<T> src)
	{
		List<T> ret = new ArrayList<T>(src.size());
		for (T val: src)
			ret.add(cloner.apply(val));
		return ret;
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
