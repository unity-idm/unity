/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Internal engine API for entities and identities management.
 * @author K. Benedyczak
 */
public interface IdentityTypesManagement
{
	/**
	 * @return list of supported identity types
	 * @throws EngineException
	 */
	public Collection<IdentityType> getIdentityTypes() throws EngineException;
	
	/**
	 * @return identity type with given name
	 * @throws EngineException
	 */
	public IdentityType getIdentityType(String name) throws EngineException;
	
	/**
	 * Allows to update mutable part of identity type, as extracted fields or description.
	 * @param toUpdate
	 * @throws EngineException
	 */
	public void updateIdentityType(IdentityType toUpdate) throws EngineException;
}

