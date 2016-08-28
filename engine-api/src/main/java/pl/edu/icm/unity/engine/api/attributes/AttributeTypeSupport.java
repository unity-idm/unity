/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * API helping to manipulate attributes, used internally
 * @author K. Benedyczak
 */
public interface AttributeTypeSupport
{
	/**
	 * @param at
	 * @return configured value syntax for the attribute type
	 */
	AttributeValueSyntax<?> getSyntax(AttributeType at);

	/**
	 * 
	 * @param attribute
	 * @return a configured attribute syntax for the given attribute name
	 */
	AttributeValueSyntax<?> getSyntax(Attribute attribute);

	/**
	 * 
	 * @param attribute
	 * @return attribute type for the attribute name
	 */
	AttributeType getType(Attribute attribute);
}