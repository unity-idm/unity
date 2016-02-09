/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Base class of all output profile action instances.
 * Ensures proper logging of action invocation.
 * @author K. Benedyczak
 */
public abstract class OutputTranslationAction extends TranslationActionInstance
{
	public OutputTranslationAction(TranslationActionType actionType, String[] parameters)
	{
		super(actionType, parameters);
	}
	
	public void invoke(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		try
		{
			NDC.push("[" + input + "]");
			invokeWrapped(input, mvelCtx, currentProfile, result);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException;
}
