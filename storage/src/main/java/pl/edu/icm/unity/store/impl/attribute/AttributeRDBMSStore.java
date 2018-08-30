/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.rdbms.cache.BasicCachingCRUD;
import pl.edu.icm.unity.store.rdbms.cache.CacheManager;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;


/**
 * RDBMS storage of {@link Attribute}
 * @author K. Benedyczak
 */
@Repository(AttributeRDBMSStore.BEAN)
public class AttributeRDBMSStore extends BasicCachingCRUD<StoredAttribute, AttributeDAO, AttributeRDBMSCache> 
					implements AttributeDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public AttributeRDBMSStore(AttributeRDBMSSerializer dbSerializer, GroupDAO groupDAO, CacheManager cacheManager)
	{
		super(new AttributeRDBMSNoCacheStore(dbSerializer, groupDAO), new AttributeRDBMSCache());
		cacheManager.registerCache(cache);
	}

	@Override
	public void updateAttribute(StoredAttribute a)
	{
		cache.flushWithEvent();
		wrapped.updateAttribute(a);
	}

	@Override
	public void deleteAttribute(String attribute, long entityId, String group)
	{
		cache.flushWithEvent();
		wrapped.deleteAttribute(attribute, entityId, group);
	}

	@Override
	public void deleteAttributesInGroup(long entityId, String group)
	{
		cache.flushWithEvent();
		wrapped.deleteAttributesInGroup(entityId, group);
	}

	@Override
	public List<StoredAttribute> getAttributes(String attribute, Long entityId, String group)
	{
		Optional<List<StoredAttribute>> cached = cache.getAttributesFiltering(
				attribute, entityId, group, this::getAll);
		if (cached.isPresent())
			return cached.get();
		return wrapped.getAttributes(attribute, entityId, group); 
	}

	@Override
	public List<AttributeExt> getEntityAttributes(long entityId, String attribute, String group)
	{
		Optional<List<StoredAttribute>> cached = cache.getAttributesFiltering(attribute, entityId, group, 
				this::getAll);
		if (cached.isPresent())
			return toAttributeExtList(cached.get());
		return wrapped.getEntityAttributes(entityId, attribute, group);
	}
	
	private List<AttributeExt> toAttributeExtList(List<StoredAttribute> src)
	{
		return src.stream().map(sa -> sa.getAttribute()).collect(Collectors.toList());
	}
}
