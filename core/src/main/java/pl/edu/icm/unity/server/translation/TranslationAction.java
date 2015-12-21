/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;


/**
 * Instance of this interface is configured with parameters and performs a translation
 * of a remotely obtained information about a client.
 * @author K. Benedyczak
 */
public interface TranslationAction
{
	/**
	 * @return the type of the action
	 */
	public TranslationActionDescription getActionDescription();
	
	/**
	 * @return the list of parameters that were used to configure the action.
	 */
	public String[] getParameters();

	
	
}
