/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.cache.Cache;

import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.types.StoredIdentity;

/**
 * Extensions of regular named cache with support for caching identities by entity
 * @author K. Benedyczak
 */
public class IdentitiesCache extends HashMapNamedCache<StoredIdentity>
{
	private Cache<Long, List<StoredIdentity>> byEntity;
	
	public IdentitiesCache(Function<StoredIdentity, StoredIdentity> cloner)
	{
		super(cloner);
	}
	
	@Override
	public synchronized void configure(int ttl, int max)
	{
		super.configure(ttl, max);
		if (disabled)
			return;
		byEntity = getBuilder(ttl, max).build();
	}

	@Override
	public synchronized void flushWithoutEvent()
	{
		if (disabled)
			return;
		super.flushWithoutEvent();
		byEntity.invalidateAll();
	}
	
	synchronized Optional<List<StoredIdentity>> getByEntity(long entityId, Runnable loader)
	{
		if (disabled)
			return Optional.empty();
		List<StoredIdentity> cached = byEntity.getIfPresent(entityId);
		if (cached == null)
			loader.run();
		cached = byEntity.getIfPresent(entityId);
		if (cached == null)
			return Optional.empty();
		
		return Optional.of(cloneList(cached));
	}
	
	@Override
	public synchronized void storeAll(List<StoredIdentity> elements)
	{
		if (disabled)
			return;
		super.storeAll(elements);
		
		byEntity.invalidateAll();
		for (StoredIdentity element: elements)
		{
			List<StoredIdentity> list = byEntity.getIfPresent(element.getEntityId());
			if (list == null)
			{
				list = new ArrayList<>();
				byEntity.put(element.getEntityId(), list);
			}
			list.add(cloner.apply(element));
		}
	}
}
