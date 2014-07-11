/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.TranslationAction;

/**
 * Instance of this interface is configured with parameters and performs a translation
 * of an exposed data to a client.
 * @author K. Benedyczak
 */
public interface OutputTranslationAction extends TranslationAction
{
	public void invoke(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException;
}
