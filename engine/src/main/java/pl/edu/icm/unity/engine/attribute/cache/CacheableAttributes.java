/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.attribute.SensitiveAttributeMatcher;

/**
 * Security sensitive attributes (credentials) are never put into the attributes cache, so they can't be read
 * from it nor found by searching it. Live computed results returned instead of cached ones, or compared with
 * them, are filtered the same way.
 */
final class CacheableAttributes
{
	private CacheableAttributes()
	{
	}

	static Map<String, AttributeExt> of(Map<String, AttributeExt> attributes)
	{
		return attributes.entrySet().stream()
				.filter(e -> !SensitiveAttributeMatcher.isSensitive(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	static Map<Long, Map<String, AttributeExt>> ofEntities(Map<Long, Map<String, AttributeExt>> attributesByEntity)
	{
		Map<Long, Map<String, AttributeExt>> ret = new HashMap<>();
		attributesByEntity.forEach((entityId, attributes) -> ret.put(entityId, of(attributes)));
		return ret;
	}
}
