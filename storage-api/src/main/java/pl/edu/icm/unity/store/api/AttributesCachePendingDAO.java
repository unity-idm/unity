/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.store.types.EntityInGroup;

/**
 * Stores the "attributes update pending" flag: for each (entity, group) pair for which the materialized
 * attributes cache ({@link AttributesCacheDAO}) is possibly stale and needs to be regenerated.
 * <p>
 * The flag is per (entity, group), not per attribute.
 */
public interface AttributesCachePendingDAO
{
	String DAO_ID = "AttributesCachePendingDAO";
	String NAME = "attributes cache pending flag";

	/**
	 * Marks the given entity in the given group as having a pending attributes update. Idempotent.
	 */
	void markPending(long entityId, String group);

	/**
	 * Marks all current members of the given group as having a pending attributes update. Idempotent.
	 */
	void markPendingForGroup(String group);

	/**
	 * Clears the pending flag of the given entity in the given group. No-op if not set.
	 */
	void clearPending(long entityId, String group);

	boolean isPending(long entityId, String group);

	List<EntityInGroup> getAll();

	List<EntityInGroup> getPendingForGroup(String group);
}
