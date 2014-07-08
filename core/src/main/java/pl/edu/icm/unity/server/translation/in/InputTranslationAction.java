/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.TranslationAction;

/**
 * Instance of this interface is configured with parameters and performs a translation
 * of a remotely obtained information about a client.
 * @author K. Benedyczak
 */
public interface InputTranslationAction extends TranslationAction
{
	/**
	 * Performs the translation.
	 * @param input
	 * @param mvelCtx context which can be used in MVEL expression evaluation
	 * @param currentProfile name of the current profile
	 * @return result of the mapping
	 * @throws EngineException when an error occurs. You can throw {@link ExecutionBreakException}
	 * to gently stop the processing of further rules.
	 */
	public MappingResult invoke(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile) throws EngineException;
}
