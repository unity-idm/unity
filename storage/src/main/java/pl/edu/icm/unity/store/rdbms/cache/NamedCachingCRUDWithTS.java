/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.types.NamedObject;

/**
 * Trivial extension of {@link NamedCachingCRUD}. For the DOA with TS we introduce no additional caching as this 
 * is nearly not used and should be anyway dropped in future Unity versions.
 */
public class NamedCachingCRUDWithTS<T extends NamedObject, DAO extends NamedCRUDDAOWithTS<T> & ReferenceAwareDAO<T>> 
		extends NamedCachingCRUD<T, DAO, NamedCache<T>> implements NamedCRUDDAOWithTS<T>, ReferenceAwareDAO<T>
{
	public NamedCachingCRUDWithTS(DAO wrapped, NamedCache<T> cache)
	{
		super(wrapped, cache);
	}

	@Override
	public List<Entry<T, Date>> getAllWithUpdateTimestamps()
	{
		return wrapped.getAllWithUpdateTimestamps();
	}

	@Override
	public List<Entry<String, Date>> getAllNamesWithUpdateTimestamps()
	{
		return wrapped.getAllNamesWithUpdateTimestamps();
	}

	@Override
	public Date getUpdateTimestamp(String name)
	{
		return wrapped.getUpdateTimestamp(name);
	}

	@Override
	public void updateTS(String id)
	{
		cache.flushWithEvent();
		wrapped.updateTS(id);
	}

	@Override
	public long createWithTS(T newValue, Date updatTS)
	{
		cache.flushWithEvent();
		return wrapped.createWithTS(newValue, updatTS);
	}

	@Override
	public void addRemovalHandler(ReferenceRemovalHandler handler)
	{
		wrapped.addRemovalHandler(handler);
	}

	@Override
	public void addUpdateHandler(ReferenceUpdateHandler<T> handler)
	{
		wrapped.addUpdateHandler(handler);
	}
}
