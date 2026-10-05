/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.AttributeSearchableValueExtractor;

/**
 * Fallback implementation, used when nothing more specific is registered (storage itself has no notion of
 * attribute value syntax plugins - see {@link AttributeSearchableValueExtractor}). The engine module
 * provides a {@code @Primary} implementation which correctly excludes binary syntaxes; this default simply
 * treats every value as searchable, so storage keeps working standalone (e.g. its own tests, which do not
 * pull in the engine module).
 */
@Component
class DefaultAttributeSearchableValueExtractor implements AttributeSearchableValueExtractor
{
	@Override
	public String getSearchableValue(String valueSyntaxId, List<String> values)
	{
		return values.isEmpty() ? null : String.join(" ", values);
	}
}
