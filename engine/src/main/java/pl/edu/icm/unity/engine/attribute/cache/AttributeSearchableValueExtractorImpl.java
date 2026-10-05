/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.store.api.AttributeSearchableValueExtractor;

/**
 * Overrides storage's {@code DefaultAttributeSearchableValueExtractor} (which cannot exclude binary
 * syntaxes, as storage has no notion of attribute value syntax plugins) wherever the engine module - and
 * so the actual syntax registry - is available.
 */
@Component
@Primary
class AttributeSearchableValueExtractorImpl implements AttributeSearchableValueExtractor
{
	private final AttributeTypeHelper attributeTypeHelper;

	@Autowired
	AttributeSearchableValueExtractorImpl(AttributeTypeHelper attributeTypeHelper)
	{
		this.attributeTypeHelper = attributeTypeHelper;
	}

	@Override
	public String getSearchableValue(String valueSyntaxId, List<String> values)
	{
		if (values.isEmpty())
			return null;
		AttributeValueSyntax<?> syntax = attributeTypeHelper.getUnconfiguredSyntax(valueSyntaxId);
		if (!syntax.isSearchable())
			return null;
		return String.join(" ", values);
	}
}
