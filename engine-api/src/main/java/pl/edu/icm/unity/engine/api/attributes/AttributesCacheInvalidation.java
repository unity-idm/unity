/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

/**
 * Marks (entity, group) pairs as having a pending materialized attributes cache update, synchronously
 * within the caller's transaction, and schedules a background refresh (see
 * {@link AttributesCacheRefreshTrigger}) once that transaction commits.
 * <p>
 * As decided for the initial implementation, precision of invalidation is intentionally conservative:
 * cross-group dependencies introduced by attribute statements (e.g. {@code extraAttributesGroup}, or
 * conditions inspecting an entity's full group/identity set) are not tracked. Instead, any change of an
 * entity's own state (attributes, identities, group memberships) invalidates ALL of that entity's groups,
 * and any change of a group- or system-wide definition (attribute statements, attribute types, attribute
 * classes) invalidates broadly rather than precisely. Gaps are expected to be caught by the self-validation
 * endpoint.
 */
public interface AttributesCacheInvalidation
{
	/**
	 * Marks the entity as pending in all groups it currently belongs to.
	 */
	void invalidateEntity(long entityId);

	/**
	 * Marks the entity as pending in the given single group only.
	 */
	void invalidateEntityInGroup(long entityId, String group);

	/**
	 * Marks all current members of the given group as pending.
	 */
	void invalidateGroup(String group);

	/**
	 * Marks every (entity, group) pair in the system as pending. Reserved for system-wide definition
	 * changes (attribute types, attribute classes) whose effect can not be scoped to a single group.
	 */
	void invalidateEverything();
}
