/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.Attribute;
import pl.edu.icm.unity.types.AttributeType;
import pl.edu.icm.unity.types.AttributesClass;
import pl.edu.icm.unity.types.EntityParam;

/**
 * Attributes management API.
 * @author K. Benedyczak
 */
public interface AttributesManagement
{
	/**
	 * @return identifiers of all attribute value types which are supported by server. 
	 * The list is constant for the lifetime of the server as is constructed from the available implementations.
	 * @throws EngineException
	 */
	public String[] getSupportedAttributeValueTypes() throws EngineException;
	
	/**
	 * Adds a new attribute type.
	 * @param at
	 * @throws EngineException
	 */
	public <T> void addAttributeType(AttributeType<T> at) throws EngineException;

	/**
	 * Updates an existing attribute type. Fails if the change break constraints of attributes
	 * already having this attribute set.
	 * @param at
	 * @throws EngineException
	 */
	public <T> void updateAttributeType(AttributeType<T> at) throws EngineException;

	/**
	 * Removes attribute type by id.
	 * @param id
	 * @param deleteInstances if false then operation will succeed only if no attributes of this type are
	 * defined. If true then also all instances of this type are removed. 
	 * @throws EngineException
	 */
	public void removeAttributeType(String id, boolean deleteInstances) throws EngineException;

	/**
	 * @return all attribute types
	 * @throws EngineException
	 */
	public List<AttributeType<?>> getAttributeTypes() throws EngineException;

	/**
	 * Defines a new attribute class
	 * @param clazz
	 * @throws EngineException
	 */
	public void addAttributeClass(AttributesClass clazz) throws EngineException;
	
	/**
	 * Removes attribute class
	 * @param id
	 * @throws EngineException
	 */
	public void removeAttributeClass(String id) throws EngineException;

	/**
	 * Updates the set of entity attribute classes. The entity must have all the requires attributes
	 * set, otherwise the operation will fail.
	 * @param entity
	 * @param classes
	 * @throws EngineException
	 */
	public void assignAttributeClasses(EntityParam entity, String[] classes) throws EngineException;
	

	/**
	 * Creates or updates an attribute.
	 * @param entity
	 * @param attribute
	 * @param update
	 * @throws EngineException
	 */
	public <T> void setAttribute(EntityParam entity, Attribute<T> attribute, boolean update) throws EngineException;
	
	/**
	 * Removes a given attribute
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @throws EngineException
	 */
	public void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId) throws EngineException;

	/**
	 * Returns visible attributes of an entity. The two last arguments can be null, meaning that there is no restriction.
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @return
	 * @throws EngineException
	 */
	public <T> List<Attribute<T>> getAttributes(EntityParam entity, String groupPath, String attributeTypeId) throws EngineException;

	/**
	 * Returns hidden attributes of an entity. The two last arguments can be null, meaning that there is no restriction.
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @return
	 * @throws EngineException
	 */
	public <T> List<Attribute<T>> getHiddenAttributes(EntityParam entity, String groupPath, String attributeTypeId) throws EngineException;

}
