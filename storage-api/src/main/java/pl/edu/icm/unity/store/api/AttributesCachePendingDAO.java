/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.store.types.EntityInGroup;
import pl.edu.icm.unity.store.types.PendingAttributesCacheEntry;

/**
 * Stores the "attributes update pending" flag: for each (entity, group) pair for which the materialized
 * attributes cache ({@link AttributesCacheDAO}) is possibly stale and needs to be regenerated.
 * <p>
 * The flag is per (entity, group), not per attribute. Each flag carries a version, bumped on every
 * (re)marking, so that a refresh computed from an older state can't clear an invalidation made after
 * the computation started.
 */
public interface AttributesCachePendingDAO
{
	String DAO_ID = "AttributesCachePendingDAO";
	String NAME = "attributes cache pending flag";

	/**
	 * Marks the given entity in the given group as having a pending attributes update. If already marked,
	 * bumps the flag's version.
	 */
	void markPending(long entityId, String group);

	/**
	 * Marks all current members of the given group as having a pending attributes update in all groups
	 * they belong to (not only in the given one). Bumps the version of the flags which were already set.
	 */
	void markPendingForGroupMembers(String group);

	/**
	 * Marks every existing group membership as having a pending attributes update. Bumps the version
	 * of the flags which were already set.
	 */
	void markPendingForAll();

	/**
	 * Clears the pending flag of the given entity in the given group, but only if it still has the given
	 * version. No-op if not set or if it was re-marked in the meantime.
	 * @return true if the flag was cleared
	 */
	boolean clearPendingIfVersion(long entityId, String group, long version);

	boolean isPending(long entityId, String group);

	List<PendingAttributesCacheEntry> getAll();

	List<EntityInGroup> getPendingForGroup(String group);
}
