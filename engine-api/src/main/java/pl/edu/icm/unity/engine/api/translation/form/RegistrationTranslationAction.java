/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.form;

import org.apache.logging.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;

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
	 * @param context which can be used to extract request related data
	 * @param mvelCtx context which can be used in MVEL expression evaluation
	 * @param currentProfile name of the current profile
	 * @return result of the mapping
	 * @throws EngineException when an error occurs. You can throw {@link ExecutionBreakException}
	 * to gently stop the processing of further rules.
	 */
	public final void invoke(TranslatedRegistrationRequest state,
			Object mvelCtx, RegistrationContext context, String currentProfile) throws EngineException
	{
		try
		{
			String identity = state.getIdentities().isEmpty() ? "unknown" : 
				state.getIdentities().iterator().next().toString();
			NDC.push("[" + identity + "]");
			invokeWrapped(state, mvelCtx, context, currentProfile);
		} catch (Exception e)
		{
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Error performing translation action", e);
			}			
			throw new EngineException("Error invoking translation action", e);
		} finally
		{
			NDC.pop();			
		}
	}
	
	protected abstract void invokeWrapped(TranslatedRegistrationRequest state,
			Object mvelCtx, RegistrationContext contexts, String currentProfile) throws EngineException;

	
	public abstract void validateGroupRestrictedForm(GroupRestrictedFormValidationContext context) throws ActionValidationException;
}

