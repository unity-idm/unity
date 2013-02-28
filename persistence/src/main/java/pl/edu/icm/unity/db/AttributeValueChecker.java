/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Provides logic to check if {@link Attribute} values are valid wrt {@link AttributeType}
 * @author K. Benedyczak
 */
public class AttributeValueChecker
{
	public static <T> void validate(Attribute<T> attribute, AttributeType at)
	{
		List<T> values = attribute.getValues();
		if (at.getMinElements() > values.size())
			throw new IllegalAttributeValueException("Attribute must have at least " + 
					at.getMinElements() + " values");
		if (at.getMaxElements() < values.size())
			throw new IllegalAttributeValueException("Attribute must have at most " + 
					at.getMaxElements() + " values");
		AttributeValueSyntax<T> valueSyntax = attribute.getAttributeSyntax();
		if (!valueSyntax.getClass().equals(at.getValueType().getClass()))
			throw new IllegalAttributeTypeException(
					"Attribute being checked has value syntax object set of class " + 
					valueSyntax.getClass() + " while its type requires " + at.getValueType());
		if (attribute.getVisibility() == AttributeVisibility.full &&
				at.getVisibility() == AttributeVisibility.local)
			throw new IllegalAttributeTypeException(
					"Can not make an attribute of hidden type public. " +
					"It is only possible to restrict visibility per-attribute");
		for (T val: values)
			valueSyntax.validate(val);
	}
}
