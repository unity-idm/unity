/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * This interface collect engine's operations related to attributes. Those operations doesn't check for authorization
 * therefore their usage should be limited by a wrapping code, the operations must not be exposed directly.
 * 
 * @author K. Benedyczak
 */
public interface AttributesInternalProcessing
{
	/**
	 * Returns attribute type which has the given metadata set. The metadata must be singleton, 
	 * otherwise unchecked exception is thrown. If there is no attribute type with this metadata, then
	 * null is returned.
	 * 
	 * @param metadataId
	 * @return
	 * @throws EngineException
	 */
	public AttributeType getAttributeTypeWithSingeltonMetadata(String metadataId) throws EngineException;

	/**
	 * Returns all attribute types which have the given metadata set.
	 * @param metadataId
	 * @return
	 * @throws EngineException
	 */
	public List<AttributeType> getAttributeTypeWithMetadata(String metadataId) throws EngineException;
	
	
	/**
	 * Returns attribute which has the given metadata set. If there is no attribute type with this metadata, then
	 * null is returned. The metadata must be singleton, 
	 * otherwise unchecked exception is thrown.
	 * @param entity
	 * @param group
	 * @param metadataId
	 * @return
	 * @throws EngineException
	 */
	public AttributeExt<?> getAttributeByMetadata(EntityParam entity, String group, String metadataId) throws EngineException;
}
