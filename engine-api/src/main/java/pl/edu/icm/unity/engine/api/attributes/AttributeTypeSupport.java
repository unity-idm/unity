/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.Collection;
import java.util.List;

import org.springframework.core.io.Resource;

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
	 * As {@link #getSyntax(Attribute)} but this method returns a syntax with default configuration
	 * for the given attribute if there is no attribute type recorded in the system.
	 * 
	 * @param attribute
	 * @return a configured attribute syntax for the given attribute name
	 */
	AttributeValueSyntax<?> getSyntaxFallingBackToDefault(Attribute attribute);
	
	/**
	 * @param attribute
	 * @return attribute type for the attribute name
	 */
	AttributeType getType(Attribute attribute);

	/**
	 * @param attribute
	 * @return attribute type for the attribute name
	 */
	AttributeType getType(String attribute);
	
	Collection<AttributeType> getAttributeTypes();
	
	/**
	 * Loads attribute types from resource
	 * @param f
	 * @return
	 */
	List<AttributeType> loadAttributeTypesFromResource(Resource f);
	
	/**
	 * Get attribute type resource from classpath resource dir
	 * @return
	 */
	List<Resource> getAttibuteTypeResourcesFromClasspathDir();
}