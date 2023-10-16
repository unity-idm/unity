/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;

@Component
public class AttributeTypeByMetaCache
{
	public final int ATRIBUTE_TYPE_CACHE_TTL_IN_MINUTES = 15; 
	
	private final AttributeTypeDAO attributeTypeDAO;
	private final AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private final Cache<String, CachedAttributeType> attributeTypeByMetaCache;

	@Autowired
	AttributeTypeByMetaCache(
			AttributeTypeDAO attributeTypeDAO,
			AttributeMetadataProvidersRegistry atMetaProvidersRegistry)
	{
		this.attributeTypeDAO = attributeTypeDAO;
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		attributeTypeByMetaCache = CacheBuilder.newBuilder()
				.expireAfterWrite(ATRIBUTE_TYPE_CACHE_TTL_IN_MINUTES, TimeUnit.MINUTES)
				.build();
	}

	AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId) throws EngineException
	{
		CachedAttributeType cached = attributeTypeByMetaCache.getIfPresent(metadataId);
		if (cached != null)
			return cached.attributeType.orElse(null);
		
		return getFreshAttributeTypeWithSingeltonMetadata(metadataId);
	}
	
	private AttributeType getFreshAttributeTypeWithSingeltonMetadata(String metadataId) throws EngineException
	{
		AttributeMetadataProvider provider = atMetaProvidersRegistry.getByName(metadataId);
		if (!provider.isSingleton())
			throw new IllegalArgumentException("Metadata " + metadataId +  "is not singleton.");
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		AttributeType ret = null;
		for (AttributeType at : existingAts)
		{	if (at.getMetadata()
					.containsKey(metadataId))
			{
				ret = at;
				break;
			}
		}
		
		attributeTypeByMetaCache.put(metadataId, new CachedAttributeType(ret));
		return ret;
	}
	
	public void clear()
	{
		attributeTypeByMetaCache.invalidateAll();
	}
	
	private static class CachedAttributeType
	{
		public final Optional<AttributeType> attributeType;

		public CachedAttributeType(AttributeType attributeType)
		{
			this.attributeType = Optional.ofNullable(attributeType);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(attributeType);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CachedAttributeType other = (CachedAttributeType) obj;
			return Objects.equals(attributeType, other.attributeType);
		}
	}
	
}
