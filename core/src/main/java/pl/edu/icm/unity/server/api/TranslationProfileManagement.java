/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;

/**
 * This interface allows clients to manipulate translation profiles. Translation profiles are 
 * used during authentication with external IdPs: the information about user is mapped to the internal Unity 
 * format and automation of updates of the local DB is controlled by the translation profiles. 
 * 
 * @author K. Benedyczak
 */
public interface TranslationProfileManagement
{
	public void addProfile(TranslationProfile toAdd) throws EngineException;
	
	public void removeProfile(String name) throws EngineException;
	
	public void updateProfile(TranslationProfile updated) throws EngineException;
	
	public Map<String, TranslationProfile> listProfiles() throws EngineException;
}
