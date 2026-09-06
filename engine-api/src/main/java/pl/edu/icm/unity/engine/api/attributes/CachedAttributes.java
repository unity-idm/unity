/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;

/**
 * Effective attributes as currently materialized in the cache, together with a flag stating whether
 * a background update of this data is pending (i.e. the returned attributes may be stale).
 */
public record CachedAttributes(Map<String, AttributeExt> attributes, boolean updatePending)
{
}
