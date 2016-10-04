/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.engine.api.attributes.AttributeParam;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Attributes management API.
 * @author K. Benedyczak
 */
public interface AttributesManagement extends BasicEngineDAO<AttributeParam, AttributeExt>
{
	/**
	 * Creates or updates an attribute.
	 * @param entity
	 * @param attribute
	 * @param update
	 * @throws EngineException
	 * 
	 * @deprecated <b>Use {@link AttributesManagement#create(AttributeParam)}
	 *             and {@link AttributesManagement#update(AttributeParam)}
	 *             instead.</b>
	 */
	@Deprecated
	void setAttribute(EntityParam entity, Attribute attribute, boolean update) throws EngineException;

	/**
	 * Removes a given attribute
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @throws EngineException
	 * 
	 * @deprecated <b>Use {@link AttributesManagement#delete(AttributeParam)} instead.</b>
	 */
	@Deprecated
	void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId) throws EngineException;

	/**
	 * Returns visible attributes of an entity. The two last arguments can be null, 
	 * meaning that there is no restriction.
	 * @param entity
	 * @param groupPath
	 * @param attributeTypeId
	 * @return
	 * @throws EngineException
	 * 
	 * @deprecated <b>Use {@link AttributesManagement#getAttributes(AttributeParam)} instead.</b>
	 */
	@Deprecated
	Collection<AttributeExt> getAttributes(EntityParam entity, String groupPath, String attributeTypeId) throws EngineException;

	/**
	 * Returns visible attributes of an entity. The two last arguments can be null, 
	 * meaning that there is no restriction.
	 * @param engineAttr
	 * @return
	 * @throws EngineException
	 */
	Collection<AttributeExt> getAttributes(AttributeParam engineAttr) throws EngineException;	

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
	 * 
	 * @deprecated <b>Use {@link AttributesManagement#getAllAttributes(AttributeParam, boolean, boolean)} instead.</b>
	 */
	@Deprecated
	Collection<AttributeExt> getAllAttributes(EntityParam entity, boolean effective, 
			String groupPath, String attributeTypeId, boolean allowDegrade) throws EngineException;

	/**
	 * Returns attributes of an entity, including hidden ones. The two last arguments can be null, 
	 * meaning that there is no restriction.
	 * @param engineAttr
	 * @param effective if false then attributes which are added by groups' attribute statements are
	 * not included. Useful only for attribute management interfaces.
	 * @param allowDegrade if true then in case that the caller has no permission to read hidden attributes,
	 * the method will degrade itself and will try to return only the visible attributes, what requires 
	 * smaller permissions. Note that still it may cause authz error.
	 * @return
	 * @throws EngineException
	 */
	Collection<AttributeExt> getAllAttributes(AttributeParam engineAttr, boolean effective, boolean allowDegrade) throws EngineException;
}
