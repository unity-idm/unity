/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import java.util.List;

import pl.edu.icm.unity.types.DescribedObject;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Profile of translation: a list of translation rules annotated with a name and description.
 * This interface could be extended with more methods, but then it would need to be generic, what 
 * causes problems with persistence. 
 * @author K. Benedyczak
 */
public interface TranslationProfile extends DescribedObject
{
	List<? extends AbstractTranslationRule<?>> getRules();
	
	String toJson(ObjectMapper jsonMapper);
	
	/**
	 * @return type of the profile
	 */
	ProfileType getProfileType();
}
