/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import org.apache.logging.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Instance of this interface is configured with parameters and performs a translation
 * of a remotely obtained information about a client.
 * @author K. Benedyczak
 */
public abstract class InputTranslationAction extends TranslationActionInstance
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationAction.class);
	
	
	public InputTranslationAction(TranslationActionType actionType, String[] parameters)
	{
		super(actionType, parameters);
	}

	/**
	 * Performs the translation.
	 * @param input
	 * @param mvelCtx context which can be used in MVEL expression evaluation
	 * @param currentProfile name of the current profile
	 * @return result of the mapping
	 * @throws EngineException when an error occurs. You can throw {@link ExecutionBreakException}
	 * to gently stop the processing of further rules.
	 */
	public final MappingResult invoke(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile) throws EngineException
	{
		try
		{
			NDC.push(input.toString());
			return invokeWrapped(input, mvelCtx, currentProfile);
		} catch (Exception e)
		{
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Error getting mapping result.", e);
			}			
			throw new EngineException(e);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile) throws EngineException;
	

}
