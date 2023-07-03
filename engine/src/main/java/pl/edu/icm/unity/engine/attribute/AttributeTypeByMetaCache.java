/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvider;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.types.basic.AttributeType;

@Component
class AttributeTypeByMetaCache
{
	private final AttributeTypeDAO attributeTypeDAO;
	private final AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	private final Cache<String, AttributeType> attributeTyeByMetaCache;

	@Autowired
	AttributeTypeByMetaCache(
			AttributeTypeDAO attributeTypeDAO,
			AttributeMetadataProvidersRegistry atMetaProvidersRegistry)
	{
		this.attributeTypeDAO = attributeTypeDAO;
		this.atMetaProvidersRegistry = atMetaProvidersRegistry;
		attributeTyeByMetaCache = CacheBuilder.newBuilder()
				.build();
	}

	AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId) throws EngineException
	{
		AttributeType cached = attributeTyeByMetaCache.getIfPresent(metadataId);
		if (cached != null)
			return cached;

		AttributeMetadataProvider provider = atMetaProvidersRegistry.getByName(metadataId);
		if (!provider.isSingleton())
			throw new IllegalArgumentException("Metadata for this call must be singleton.");
		Collection<AttributeType> existingAts = attributeTypeDAO.getAll();
		AttributeType ret = null;
		for (AttributeType at : existingAts)
			if (at.getMetadata()
					.containsKey(metadataId))
				ret = at;
		if (ret != null)
		{
			attributeTyeByMetaCache.put(metadataId, ret);
		}

		return ret;
	}

	void clear()
	{
		attributeTyeByMetaCache.invalidateAll();
	}
}
