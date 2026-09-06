/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.List;

/**
 * Self-validation result of the materialized attributes cache, per group.
 */
public record AttributeCacheValidationReport(List<GroupValidationReport> groups)
{
}
