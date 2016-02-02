/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ExecutionBreakException;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Base of all actions operating on a contents of a registration request submitted by a prospective user. 
 * Wraps of invocation with logging and exception protection.
 * @author K. Benedyczak
 */
public abstract class RegistrationTranslationAction extends TranslationActionInstance 
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_TRANSLATION, 
			RegistrationTranslationAction.class);
	
	public RegistrationTranslationAction(TranslationActionType actionType, String[] parameters)
	{
		super(actionType, parameters);
	}

	/**
	 * Performs the translation.
	 * @param state
	 * @param mvelCtx context which can be used in MVEL expression evaluation
	 * @param currentProfile name of the current profile
	 * @return result of the mapping
	 * @throws EngineException when an error occurs. You can throw {@link ExecutionBreakException}
	 * to gently stop the processing of further rules.
	 */
	public final void invoke(TranslatedRegistrationRequest state,
			Object mvelCtx,	String currentProfile) throws EngineException
	{
		try
		{
			String identity = state.getIdentities().isEmpty() ? "unknown" : 
				state.getIdentities().iterator().next().toString();
			NDC.push("[" + identity + "]");
			invokeWrapped(state, mvelCtx, currentProfile);
		} catch (Exception e)
		{
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Error performing translation action", e);
			}			
			throw new EngineException(e);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract void invokeWrapped(TranslatedRegistrationRequest state,
			Object mvelCtx,	String currentProfile) throws EngineException;

}
