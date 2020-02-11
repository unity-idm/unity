/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

/**
 * Used in conjunction with attribute syntax
 * {@link AttributeValueSyntax#shareSpec()} implementation.
 * 
 * Provides information consumed by unity on what is the attribute's value
 * unique id, and how it translates to "binary" content.
 */
public interface SharedAttributeSpec
{
	SharedAttributeInfo getInfo(String stringRepresentation);

	SharedAttributeContentProvider getContentProvider();

	interface SharedAttributeContentProvider
	{
		SharedAttributeContent getContent(String stringRepresentation);
	}
}
