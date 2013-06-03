/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Preferences management API - allows for storing and retrieving user's preferences,
 * useful for example for the web endpoints.
 * 
 * @author K. Benedyczak
 */
public interface PreferencesManagement
{
	/**
	 * Sets or updates a preference for the specified user. The value can be arbitrary, for instance
	 * JSON encoded.
	 * @param entity
	 * @param preferenceId
	 * @param value
	 * @throws EngineException
	 */
	public void setPreference(EntityParam entity, String preferenceId, String value) throws EngineException; 
	
	/**
	 * Returns a given preference. Null is returned if there is no such preference.
	 * @param entity
	 * @param preferenceId
	 * @return
	 * @throws EngineException
	 */
	public String getPreference(EntityParam entity, String preferenceId) throws EngineException;
	
	/**
	 * Removes the given preference.
	 * @param entity
	 * @param preferenceId
	 * @throws EngineException
	 */
	public void removePreference(EntityParam entity, String preferenceId) throws EngineException;
}
