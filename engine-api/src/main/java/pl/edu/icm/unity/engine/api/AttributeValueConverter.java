/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

/**
 * Performs conversion of {@link Attribute} values in a convenient to use way.
 * Under the hood delegates to proper attribute syntax.
 */
public interface AttributeValueConverter
{
	List<String> externalValuesToInternal(String attributeName, List<?> externalValues)
			throws IllegalAttributeValueException;

	<T> List<String> externalValuesToInternal(AttributeValueSyntax<T> syntax, List<?> externalValues)
			throws IllegalAttributeValueException;

	<T> List<String> internalValuesToExternal(AttributeValueSyntax<T> syntax, List<String> internalValues);

	List<String> internalValuesToExternal(String attributeName, List<String> internalValues);

	List<?> internalValuesToObjectValues(String attributeName, List<String> internalValues)
			throws IllegalAttributeValueException;

	<T> List<T> internalValuesToObjectValues(AttributeValueSyntax<T> syntax, List<String> internalValues)
			throws IllegalAttributeValueException;

	<T> List<String> objectValuesToInternalValues(AttributeValueSyntax<T> syntax, List<T> typedValues)
			throws IllegalAttributeValueException;
}
