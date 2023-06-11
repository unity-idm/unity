/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Collection;
import java.util.List;

import org.springframework.core.io.Resource;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;

/**
 * API helping to manipulate attributes, used internally
 * @author K. Benedyczak
 */
public interface AttributeTypeSupport
{
	/**
	 * @return configured value syntax for the attribute type
	 */
	AttributeValueSyntax<?> getSyntax(AttributeType at);

	/**
	 * @return a configured attribute syntax for the given attribute name
	 */
	AttributeValueSyntax<?> getSyntax(Attribute attribute);
	
	/**
	 * As {@link #getSyntax(Attribute)} but this method returns a syntax with default configuration
	 * for the given attribute if there is no attribute type recorded in the system.
	 * 
	 * @return a configured attribute syntax for the given attribute name
	 */
	AttributeValueSyntax<?> getSyntaxFallingBackToDefault(Attribute attribute);
	
	/**
	 * @return returned syntax is a default instance
	 */
	AttributeValueSyntax<?> getUnconfiguredSyntax(String syntaxId);
	
	/**
	 * @return attribute type for the attribute name
	 */
	AttributeType getType(Attribute attribute);

	/**
	 * @return attribute type for the attribute name
	 */
	AttributeType getType(String attribute);
	
	Collection<AttributeType> getAttributeTypes();
	
	/**
	 * Loads attribute types from resource
	 */
	List<AttributeType> loadAttributeTypesFromResource(Resource f);
	
	/**
	 * Get attribute type resource from classpath resource dir
	 */
	List<Resource> getAttibuteTypeResourcesFromClasspathDir();
}