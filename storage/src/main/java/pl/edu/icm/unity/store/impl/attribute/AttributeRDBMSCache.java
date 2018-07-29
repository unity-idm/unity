/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pl.edu.icm.unity.store.rdbms.cache.HashMapBasicCache;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Cache dedicated to attributes, adds handling of complex searching of attributes. 
 *  
 * @author K. Benedyczak
 */
class AttributeRDBMSCache extends HashMapBasicCache<StoredAttribute> 
{
	private Map<Long, List<StoredAttribute>> allByEntity = new HashMap<>();
	
	AttributeRDBMSCache()
	{
		super(sa -> new StoredAttribute(sa));
	}
	
	@Override
	public synchronized void flush()
	{
		super.flush();
		allByEntity.clear();
	}
	
	@Override
	public synchronized void storeAll(List<StoredAttribute> elements)
	{
		super.storeAll(elements);
		allByEntity.clear();
		for (StoredAttribute sa: elements)
		{
			List<StoredAttribute> list = allByEntity.get(sa.getEntityId());
			if (list == null)
			{
				list = new ArrayList<>();
				allByEntity.put(sa.getEntityId(), list);
			}
			list.add(cloner.apply(sa));
		}
			
	}
	
	synchronized Optional<List<StoredAttribute>> getAttributesFiltering(String attribute, Long entityId, String group)
	{
		if (!cacheComplete)
			return Optional.empty();
		
		
		List<StoredAttribute> ret;
		if (entityId != null)
		{
			List<StoredAttribute> list = allByEntity.get(entityId);
			ret = new ArrayList<>();
			if (list != null)
				ret.addAll(list);
		} else
		{
			ret = new ArrayList<>(all);
		}
		if (attribute != null)
			filterByAttribute(attribute, ret);
		if (group != null)
			filterByGroup(group, ret);
		for (int i=0; i<ret.size(); i++)
			ret.set(i, cloner.apply(ret.get(i)));
		return Optional.of(ret);
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
