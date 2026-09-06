/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Groups a flat list of {@link StoredAttribute} (as returned by {@link pl.edu.icm.unity.store.api.AttributesCacheDAO})
 * by owning entity.
 */
final class StoredAttributesGrouping
{
	private StoredAttributesGrouping()
	{
	}

	static Map<Long, Map<String, AttributeExt>> byEntity(List<StoredAttribute> attributes)
	{
		Map<Long, Map<String, AttributeExt>> ret = new HashMap<>();
		for (StoredAttribute sa : attributes)
			ret.computeIfAbsent(sa.getEntityId(), k -> new HashMap<>())
					.put(sa.getAttribute().getName(), sa.getAttribute());
		return ret;
	}
}
