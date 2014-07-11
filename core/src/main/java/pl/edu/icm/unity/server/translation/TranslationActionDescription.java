/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;


/**
 * Provides a description of translation action type.
 * @author K. Benedyczak
 */
public interface TranslationActionDescription
{
	public ProfileType getSupportedProfileType();
	
	/**
	 * @return message bundle key of the action's description
	 */
	public String getDescriptionKey();
	
	/**
	 * @return name of the action
	 */
	public String getName();
	
	/**
	 * @return declaration of supported parameters
	 */
	public ActionParameterDesc[] getParameters();
}
