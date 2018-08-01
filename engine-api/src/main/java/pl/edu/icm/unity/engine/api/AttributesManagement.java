/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Attributes management API.
 * @author K. Benedyczak
 */
public interface AttributesManagement
{
	/**
	 * @deprecated use any of other create or set methods. Left as may be popular in groovy scripts 
	 * around
	 */
	@Deprecated
	void setAttribute(EntityParam entity, Attribute attribute, boolean allowUpdate) throws EngineException;
	
	/**
	 * Creates an attribute (must not be present). Confirmation will be sent if needed for attribute.
	 * @param entity
	 * @param attribute
	 * @param update
	 * @throws EngineException
	 */
	void createAttribute(EntityParam entity, Attribute attribute) throws EngineException;

	/**
	 * Updates or creates an attribute (may be present). Confirmation will be sent if needed for attribute.
	 * @param entity
	 * @param attribute
	 * @param update
	 * @throws EngineException
	 */
	void setAttribute(EntityParam entity, Attribute attribute) throws EngineException;

	/**
	 * Creates an attribute (must not be present). Confirmation will not be sent.
	 * @param entity
	 * @param attribute
	 * @param update
	 * @throws EngineException
	 */
	void createAttributeSuppressingConfirmation(EntityParam entity, Attribute attribute) throws EngineException;

	/**
	 * Updates or creates an attribute (may be present). Confirmation will not be sent.
	 * @param entity
	 * @param attribute
	 * @param update
	 * @throws EngineException
	 */
	void setAttributeSuppressingConfirmation(EntityParam entity, Attribute attribute) throws EngineException;

	
	/**
	 * Removes a given attribute
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @throws EngineException
	 */
	void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId) throws EngineException;

	/**
	 * Returns visible attributes of an entity. The two last arguments can be null, 
	 * meaning that there is no restriction. The security sensitive attributes are not included.
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @return
	 * @throws EngineException
	 */
	Collection<AttributeExt> getAttributes(EntityParam entity,  
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
	Collection<AttributeExt> getAllAttributes(EntityParam entity, boolean effective, 
			String groupPath, String attributeTypeId, boolean allowDegrade) throws EngineException;
}
