/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Internal API to access effective (materialized) attributes data. No operation in this interface
 * performs any authorization - same convention as {@link pl.edu.icm.unity.engine.attribute.AttributesHelper}.
 * <p>
 * Two variants are provided:
 * <ul>
 * <li><b>consistent</b>: returned from the materialized cache if it is fresh for all requested entities,
 * otherwise computed on the fly (using the standard bulk/single entity attributes resolution code) before
 * being returned. Never stale, but potentially slow.</li>
 * <li><b>fast</b>: always returned from the materialized cache, together with a per-entity flag stating
 * whether an update of that entity's cache is pending (i.e. whether the returned data may be stale).</li>
 * </ul>
 */
public interface EffectiveAttributesCacheService
{
	Map<String, AttributeExt> getAttributes(long entityId, String group) throws EngineException;

	Map<Long, Map<String, AttributeExt>> getGroupAttributes(String group) throws EngineException;

	CachedAttributes getAttributesFast(long entityId, String group);

	Map<Long, CachedAttributes> getGroupAttributesFast(String group);
}
