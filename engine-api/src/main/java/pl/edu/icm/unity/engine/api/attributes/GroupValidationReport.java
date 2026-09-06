/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.List;

/**
 * Self-validation result of the materialized attributes cache for a single group.
 *
 * @param entitiesWithPendingUpdate entities whose cache update was pending at the time of validation
 * (their cache is known to be possibly stale, so it was not compared against the freshly computed values).
 * @param entitiesWithMismatches entities without a pending update, for which at least one cached
 * attribute value differed from the freshly computed one - i.e. a real inconsistency, not just an
 * update in progress.
 */
public record GroupValidationReport(String group, List<Long> entitiesWithPendingUpdate,
		List<EntityAttributeMismatches> entitiesWithMismatches)
{
}
