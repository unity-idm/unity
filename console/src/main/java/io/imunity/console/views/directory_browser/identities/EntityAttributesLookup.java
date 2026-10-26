/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;

/**
 * Lazily fetches, and keeps until the next {@link #reset(String)}, the effective attributes of entities shown
 * in the identities grid - both in the root group and in the currently browsed group - together with their
 * "update pending" flags. Each entity is queried at most once per group, regardless of how many grid rows
 * (identities) or attribute columns refer to it (UY-1483).
 */
class EntityAttributesLookup
{
	private final EffectiveAttributesCacheService effectiveAttributesCacheService;
	private final Map<Long, Map<String, AttributeExt>> rootAttributes = new HashMap<>();
	private final Map<Long, Map<String, AttributeExt>> currentAttributes = new HashMap<>();
	private final Map<Long, Boolean> pending = new HashMap<>();
	private String groupPath;

	EntityAttributesLookup(EffectiveAttributesCacheService effectiveAttributesCacheService)
	{
		this.effectiveAttributesCacheService = effectiveAttributesCacheService;
	}

	void reset(String groupPath)
	{
		this.groupPath = groupPath;
		rootAttributes.clear();
		currentAttributes.clear();
		pending.clear();
	}

	Map<String, AttributeExt> getRootAttributes(long entityId)
	{
		return rootAttributes.computeIfAbsent(entityId,
				id -> effectiveAttributesCacheService.getAttributesFast(id, "/").attributes());
	}

	Map<String, AttributeExt> getCurrentAttributes(long entityId)
	{
		if ("/".equals(groupPath))
			return getRootAttributes(entityId);
		return currentAttributes.computeIfAbsent(entityId,
				id -> effectiveAttributesCacheService.getAttributesFast(id, groupPath).attributes());
	}

	/**
	 * @return the attribute shown in the given attribute column (root or current group one), or null
	 */
	AttributeExt getColumnAttribute(long entityId, String columnId)
	{
		Map<String, AttributeExt> attributes = IdentitiesGridColumnConstants.isRootAttributeColumn(columnId)
				? getRootAttributes(entityId)
				: getCurrentAttributes(entityId);
		return attributes.get(IdentitiesGridColumnConstants.getAttributeName(columnId));
	}

	/**
	 * Whether the effective attributes of the given entity in the currently browsed group may be stale
	 * (a background recalculation is pending). Cheap: reads only the pending flag, not the attributes.
	 */
	boolean isPending(long entityId)
	{
		return pending.computeIfAbsent(entityId,
				id -> effectiveAttributesCacheService.isUpdatePending(id, groupPath));
	}
}
