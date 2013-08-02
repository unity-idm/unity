/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.EntityParam;

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
	public void addAttributeType(AttributeType at) throws EngineException;

	/**
	 * Updates an existing attribute type. Fails if the change break constraints of attributes
	 * already having this attribute set.
	 * @param at
	 * @throws EngineException
	 */
	public void updateAttributeType(AttributeType at) throws EngineException;

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
	public List<AttributeType> getAttributeTypes() throws EngineException;

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
	 * Updates an attribute class. The update operation will be successful only if all entities with this class
	 * fulfill the updated class rules.
	 * @param updated the updated class. Existing class to be updated is matched by name.
	 * @throws EngineException
	 */
	public void updateAttributeClass(AttributesClass updated) throws EngineException;

	/**
	 * @return all currently defined {@link AttributesClass}es.
	 * @throws EngineException
	 */
	public Collection<AttributesClass> getAttributeClasses() throws EngineException;
	
	/**
	 * Updates the set of entity's attribute classes in a given group. 
	 * The entity must have all the requires attributes set and must not have any disallowed attributes,
	 * otherwise the operation will fail.
	 * @param entity
	 * @param classes
	 * @throws EngineException
	 */
	public void setEntityAttributeClasses(EntityParam entity, String group, Collection<String> classes) 
			throws EngineException;
	
	/**
	 * Attribute classes of a given entity in a group
	 * @param entity
	 * @param group
	 * @return
	 * @throws EngineException
	 */
	public Collection<AttributesClass> getEntityAttributeClasses(EntityParam entity, String group) 
			throws EngineException;
	

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
	public Collection<AttributeExt<?>> getAttributes(EntityParam entity,  
			String groupPath, String attributeTypeId) throws EngineException;

	/**
	 * Returns attributes of an entity, including hidden ones. The two last arguments can be null, 
	 * meaning that there is no restriction.
	 * @param entity
	 * @param effective if false then attributes which are added by groups' attribute statements are
	 * not included. Useful only for attribute management interfaces.
	 * @param groupPath
	 * @param attributeTypeId
	 * @param allowDegrade if true then in case that the caller has no permission to read hidden attributes,
	 * the method will degrade itself and will try to return only the visible attributes, what requires 
	 * smaller permissions. Note that still it may cause authz error.
	 * @return
	 * @throws EngineException
	 */
	public Collection<AttributeExt<?>> getAllAttributes(EntityParam entity, boolean effective, 
			String groupPath, String attributeTypeId, boolean allowDegrade) throws EngineException;

}
