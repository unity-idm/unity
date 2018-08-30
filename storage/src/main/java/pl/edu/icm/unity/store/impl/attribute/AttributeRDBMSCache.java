/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.google.common.cache.Cache;

import pl.edu.icm.unity.store.rdbms.cache.GuavaBasicCache;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Cache dedicated to attributes, adds handling of complex searching of attributes. 
 *  
 * @author K. Benedyczak
 */
class AttributeRDBMSCache extends GuavaBasicCache<StoredAttribute> 
{
	private Cache<Long, List<StoredAttribute>> allByEntity;
	private boolean cacheComplete;
	
	AttributeRDBMSCache()
	{
		super(sa -> new StoredAttribute(sa));
	}
	
	@Override
	public synchronized void configure(int ttl, int max)
	{
		super.configure(ttl, max);
		if (disabled)
			return;
		allByEntity = getBuilder(ttl, max).build();
	}
	
	@Override
	public synchronized void flushWithoutEvent()
	{
		if (disabled)
			return;
		super.flushWithoutEvent();
		allByEntity.invalidateAll();
		cacheComplete = false;
	}
	
	@Override
	public synchronized void storeAll(List<StoredAttribute> elements)
	{
		if (disabled)
			return;
		super.storeAll(elements);
		allByEntity.invalidateAll();
		for (StoredAttribute sa: elements)
		{
			List<StoredAttribute> list = allByEntity.getIfPresent(sa.getEntityId());
			if (list == null)
			{
				list = new ArrayList<>();
				allByEntity.put(sa.getEntityId(), list);
			}
			list.add(cloner.apply(sa));
		}
		cacheComplete = true;
	}
	
	synchronized Optional<List<StoredAttribute>> getAttributesFiltering(String attribute, Long entityId, String group,
			Runnable loader)
	{
		if (disabled)
			return Optional.empty();
		if (!cacheComplete)
			loader.run();
		if (!cacheComplete)
			return Optional.empty();
		
		List<StoredAttribute> ret;
		if (entityId != null)
		{
			List<StoredAttribute> list = allByEntity.getIfPresent(entityId);
			ret = new ArrayList<>();
			if (list != null)
				ret.addAll(list);
		} else
		{
			ret = new ArrayList<>(all.getIfPresent(SINGLE_ENTRY_KEY));
		}
		if (attribute != null)
			filterByAttribute(attribute, ret);
		if (group != null)
			filterByGroup(group, ret);
		return Optional.of(cloneList(ret));
	}

	private void filterByAttribute(String attribute, List<StoredAttribute> ret)
	{
		Iterator<StoredAttribute> iterator = ret.iterator();
		while(iterator.hasNext())
			if (!iterator.next().getAttribute().getName().equals(attribute))
				iterator.remove();
	}

	private void filterByGroup(String group, List<StoredAttribute> ret)
	{
		Iterator<StoredAttribute> iterator = ret.iterator();
		while(iterator.hasNext())
			if (!iterator.next().getAttribute().getGroupPath().equals(group))
				iterator.remove();
	}
}
