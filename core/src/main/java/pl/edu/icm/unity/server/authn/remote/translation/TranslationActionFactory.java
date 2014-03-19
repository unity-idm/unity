/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Creates {@link TranslationAction}s and provides a description of the created actions.
 * @author K. Benedyczak
 */
public interface TranslationActionFactory
{
	public String getDescriptionKey();
	public String getName();
	public ActionParameterDesc[] getParameters();
	
	public TranslationAction getInstance(String... parameters) throws EngineException;
}
