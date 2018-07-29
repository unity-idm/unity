/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import pl.edu.icm.unity.store.rdbms.cache.HashMapNamedCache;
import pl.edu.icm.unity.store.types.StoredIdentity;

/**
 * Extensions of regular named cache with support for caching identities by entity
 * @author K. Benedyczak
 */
public class IdentitiesCache extends HashMapNamedCache<StoredIdentity>
{
	private Map<Long, List<StoredIdentity>> byEntity = new HashMap<>();
	
	public IdentitiesCache(Function<StoredIdentity, StoredIdentity> cloner)
	{
		super(cloner);
	}

	@Override
	public synchronized void flush()
	{
		super.flush();
		byEntity.clear();
	}
	
	synchronized Optional<List<StoredIdentity>> getByEntity(long entityId)
	{
		List<StoredIdentity> cached = byEntity.get(entityId);
		if (cached == null)
			return Optional.empty();
		List<StoredIdentity> ret = new ArrayList<>(cached.size());
		for (StoredIdentity c: cached)
			ret.add(cloner.apply(c));
		return Optional.of(ret);
	}
	
	@Override
	public synchronized void storeAll(List<StoredIdentity> elements)
	{
		super.storeAll(elements);
		
		byEntity.clear();
		for (StoredIdentity element: elements)
		{
			List<StoredIdentity> list = byEntity.get(element.getEntityId());
			if (list == null)
			{
				list = new ArrayList<>();
				byEntity.put(element.getEntityId(), list);
			}
			list.add(cloner.apply(element));
		}
	}
}
