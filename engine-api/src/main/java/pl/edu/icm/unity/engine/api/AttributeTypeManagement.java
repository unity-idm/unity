/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Attribute types management API.
 * @author K. Benedyczak
 */
public interface AttributeTypeManagement
{
	/**
	 * @return identifiers of all attribute value types which are supported by server. 
	 * The list is constant for the lifetime of the server as is constructed from the available implementations.
	 */
	String[] getSupportedAttributeValueTypes() throws EngineException;
	
	/**
	 * Adds a new attribute type.
	 */
	void addAttributeType(AttributeType at) throws EngineException;

	/**
	 * Updates an existing attribute type. Fails if the change break constraints of attributes
	 * already having this attribute set.
	 */
	void updateAttributeType(AttributeType at) throws EngineException;

	/**
	 * Removes attribute type by id.
	 * @param deleteInstances if false then operation will succeed only if no attributes of this type are
	 * defined. If true then also all instances of this type are removed. 
	 */
	void removeAttributeType(String id, boolean deleteInstances) throws EngineException;

	/**
	 * @return all attribute types
	 */
	Collection<AttributeType> getAttributeTypes() throws EngineException;

	/**
	 * @return all attribute types map with names as keys
	 */
	Map<String, AttributeType> getAttributeTypesAsMap() throws EngineException;
	
	/**
	 * @param name attribute name 
	 * @return attribute type with a given name
	 */
	AttributeType getAttributeType(String name) throws EngineException;
}
