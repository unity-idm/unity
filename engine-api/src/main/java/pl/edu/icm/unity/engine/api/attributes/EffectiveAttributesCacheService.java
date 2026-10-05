/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Internal API to access effective (materialized) attributes data. Every method checks the caller's
 * authorization for the given group ({@code readHidden} or {@code read}), same as {@link
 * pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService}.
 * <p>
 * Two variants are provided:
 * <ul>
 * <li><b>consistent</b>: returned from the materialized cache if it is fresh for all requested entities,
 * otherwise computed on the fly (using the standard bulk/single entity attributes resolution code) before
 * being returned. Never stale, but potentially slow.</li>
 * <li><b>fast</b>: always returned from the materialized cache, together with a per-entity flag stating
 * whether an update of that entity's cache is pending (i.e. whether the returned data may be stale). The
 * authorization failure on this variant is an unchecked {@link
 * pl.edu.icm.unity.engine.api.authn.AuthorizationExceptionRT}, so its no-checked-exception signature is
 * preserved.</li>
 * </ul>
 */
public interface EffectiveAttributesCacheService
{
	Map<String, AttributeExt> getAttributes(long entityId, String group) throws EngineException;

	Map<Long, Map<String, AttributeExt>> getGroupAttributes(String group) throws EngineException;

	CachedAttributes getAttributesFast(long entityId, String group);

	Map<Long, CachedAttributes> getGroupAttributesFast(String group);

	/**
	 * Cheap check of the per-entity pending flag alone, without reading or deserializing the cached
	 * attributes themselves. Use this when only the "may be stale" status is needed (e.g. to decide
	 * whether to show a staleness indicator in the UI), not the attribute values.
	 */
	boolean isUpdatePending(long entityId, String group);
}
