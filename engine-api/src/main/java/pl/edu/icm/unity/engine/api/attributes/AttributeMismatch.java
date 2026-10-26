/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.List;

/**
 * A single attribute whose cached value differs from the freshly computed one.
 *
 * @param cachedValues the (incorrect) values currently stored in the materialized attributes cache.
 * @param freshValues the (correct) values as freshly computed.
 * @param valuesRedacted if true, the attribute is sensitive (e.g. holds credential data) and both value lists
 * are intentionally left empty.
 */
public record AttributeMismatch(String attributeName, List<String> cachedValues, List<String> freshValues,
		boolean valuesRedacted)
{
	public static AttributeMismatch redacted(String attributeName)
	{
		return new AttributeMismatch(attributeName, List.of(), List.of(), true);
	}
}
