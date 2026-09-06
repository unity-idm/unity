/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

/**
 * Published (synchronously, via Spring's {@link org.springframework.context.ApplicationEventPublisher})
 * whenever an entity's own state (attributes, identities) changes in a way that may affect its
 * effective attributes. Kept as a plain, dependency-free event so that low-level helpers (in particular
 * {@link pl.edu.icm.unity.engine.attribute.AttributesHelper}) do not need to depend directly on the
 * attributes cache machinery, which would otherwise create a circular bean dependency (the cache refresh
 * background job depends, transitively, on those same low-level helpers to compute effective attributes).
 */
public record EntityAttributesChangedEvent(long entityId)
{
}
