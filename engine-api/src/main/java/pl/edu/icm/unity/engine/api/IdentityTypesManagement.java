/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Internal engine API for entities and identities management.
 * @author K. Benedyczak
 */
public interface IdentityTypesManagement extends NamedEngineDAO<IdentityType>
{
	/**
	 * @return list of supported identity types
	 * @throws EngineException
	 * 
	 * @deprecated <b>Use {@link IdentityTypesManagement#getAll()} instead.</b>
	 */
	@Deprecated
	public Collection<IdentityType> getIdentityTypes() throws EngineException;
	
	/**
	 * Allows to update mutable part of identity type, as extracted fields or description.
	 * @param toUpdate
	 * @throws EngineException
	 * 
	 * @deprecated <b>Use {@link IdentityTypesManagement#update(IdentityType)} instead.</b>
	 */
	@Deprecated
	public void updateIdentityType(IdentityType toUpdate) throws EngineException;
}

