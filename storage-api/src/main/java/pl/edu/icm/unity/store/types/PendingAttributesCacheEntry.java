/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.types;

/**
 * A pending attributes cache update of an entity in a group. The version is bumped on each invalidation,
 * so that a refresh can clear only the invalidation it has actually processed.
 */
public record PendingAttributesCacheEntry(long entityId, String group, long version)
{
}
