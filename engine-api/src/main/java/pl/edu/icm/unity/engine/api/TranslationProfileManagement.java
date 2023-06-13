/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Map;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;

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
	public static final String DEFAULT_OUTPUT_PROFILE = "sys:default";
	
	void addProfile(TranslationProfile toAdd) throws EngineException;
	
	void removeProfile(ProfileType type, String name) throws EngineException;
	
	void updateProfile(TranslationProfile updated) throws EngineException;

	TranslationProfile getInputProfile(String name) throws EngineException;

	TranslationProfile getOutputProfile(String name) throws EngineException;

	Map<String, TranslationProfile> listInputProfiles() throws EngineException;

	Map<String, TranslationProfile> listOutputProfiles() throws EngineException;
}
