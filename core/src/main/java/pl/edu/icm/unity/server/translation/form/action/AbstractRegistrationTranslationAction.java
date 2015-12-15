/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Wrapping of invoke with logging and exception protection.
 * @author K. Benedyczak
 */
public abstract class AbstractRegistrationTranslationAction extends AbstractTranslationAction 
	implements RegistrationTranslationAction
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_TRANSLATION, 
			AbstractRegistrationTranslationAction.class);
	
	public AbstractRegistrationTranslationAction(TranslationActionDescription description,
			String[] params)
	{
		super(description, params);
	}

	@Override
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
