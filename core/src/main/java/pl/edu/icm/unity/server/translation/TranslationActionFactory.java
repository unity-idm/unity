/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Creates {@link TranslationActionInstance}s.
 * @author K. Benedyczak
 */
public interface TranslationActionFactory
{
	/**
	 * @return definition of an action created by this factory
	 */
	TranslationActionType getActionType();
	
	/**
	 * Actual factory method
	 * @param parameters parameter values.
	 * @return configured instance
	 * @throws EngineException
	 */
	public TranslationActionInstance getInstance(String... parameters);
}
