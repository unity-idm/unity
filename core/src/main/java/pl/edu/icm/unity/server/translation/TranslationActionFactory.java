/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Creates {@link TranslationAction}s and provides a description of the created actions.
 * @author K. Benedyczak
 */
public interface TranslationActionFactory extends TranslationActionDescription
{
	/**
	 * Actual factory method
	 * @param parameters parameter values.
	 * @return configured instance
	 * @throws EngineException
	 */
	TranslationAction getInstance(String... parameters);
	
	/**
	 * Used when an exception is thrown by the base {@link #getInstance(String...)} method. 
	 * Must not throw any exception (naturally besides {@link Error} ;-)). The returned action 
	 * should generally do nothing besides logging that it is a blind stopper of the real action.
	 * @param parameters
	 * @return
	 */
	TranslationAction getBlindInstance(String... parameters);
}
