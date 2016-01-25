/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;

/**
 * This interface allows clients to manipulate translation profiles. Translation profiles are 
 * used to manipulate entities data in various moments of Unity usage. Each profile has its type
 * which defines the place where it can be used. For instance the input profiles are used to manipulate the incoming
 * data from external IdPs. 
 * 
 * @author K. Benedyczak
 */
public interface TranslationProfileManagement
{
	void addProfile(TranslationProfile<?> toAdd) throws EngineException;
	
	void removeProfile(String name) throws EngineException;
	
	void updateProfile(TranslationProfile<?> updated) throws EngineException;
	
	Map<String, InputTranslationProfile> listInputProfiles() throws EngineException;

	Map<String, OutputTranslationProfile> listOutputProfiles() throws EngineException;

	OutputTranslationProfile getDefaultOutputProfile() throws EngineException;
}
