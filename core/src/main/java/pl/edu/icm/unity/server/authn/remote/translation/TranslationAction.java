/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

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
	 * Performs the translation.
	 * @param input
	 * @param mvelCtx context which can be used in MVEL expression evaluation
	 * @return result of the mapping
	 * @throws EngineException when an error occurs. You can throw {@link ExecutionBreakException}
	 * to gently stop the processing of further rules.
	 */
	public MappingResult invoke(RemotelyAuthenticatedInput input, Object mvelCtx) throws EngineException;
	
	/**
	 * @return the list of parameters that were used to configure the action.
	 */
	public String[] getParameters();

}
