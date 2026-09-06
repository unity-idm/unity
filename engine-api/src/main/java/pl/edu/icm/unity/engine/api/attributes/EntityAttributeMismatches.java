/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.List;

/**
 * An entity (without a pending cache update) for which at least one cached attribute value differs
 * from the freshly computed one.
 */
public record EntityAttributeMismatches(long entityId, List<AttributeMismatch> mismatches)
{
}
