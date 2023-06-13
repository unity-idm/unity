/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;
import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;

/**
 * Attribute class management API.
 * @author K. Benedyczak
 */
public interface AttributeClassManagement
{
	/**
	 * Defines a new attribute class
	 * @param clazz
	 * @throws EngineException
	 */
	void addAttributeClass(AttributesClass clazz) throws EngineException;
	
	/**
	 * Removes attribute class
	 * @param id
	 * @throws EngineException
	 */
	void removeAttributeClass(String id) throws EngineException;

	/**
	 * Updates an attribute class. The update operation will be successful only if all entities with this class
	 * fulfill the updated class rules.
	 * @param updated the updated class. Existing class to be updated is matched by name.
	 * @throws EngineException
	 */
	void updateAttributeClass(AttributesClass updated) throws EngineException;

	/**
	 * @return all currently defined {@link AttributesClass}es, keys are ac names.
	 * @throws EngineException
	 */
	Map<String, AttributesClass> getAttributeClasses() throws EngineException;
	
	/**
	 * Updates the set of entity's attribute classes in a given group. 
	 * The entity must have all the requires attributes set and must not have any disallowed attributes,
	 * otherwise the operation will fail.
	 * @param entity
	 * @param classes
	 * @throws EngineException
	 */
	void setEntityAttributeClasses(EntityParam entity, String group, Collection<String> classes) 
			throws EngineException;
	
	/**
	 * Attribute classes of a given entity in a group
	 * @param entity
	 * @param group
	 * @return
	 * @throws EngineException
	 */
	Collection<AttributesClass> getEntityAttributeClasses(EntityParam entity, String group) 
			throws EngineException;	
	
	/**
	 * @return attribute class with given name
	 * @throws EngineException
	 */
	AttributesClass getAttributeClass(String name) throws EngineException;
}
