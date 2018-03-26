/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.ExecutionFailException;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Fails the authentication. Allows for implementing poorman's authZ. 
 *   
 * @author K. Benedyczak
 */
@Component
public class FailAuthnActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "failAuthentication";
	
	public FailAuthnActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"message",
				"TranslationAction.failAuthentication.paramDesc.message",
				Type.LARGE_TEXT, true));
	}
	
	@Override
	public FailAuthnAction getInstance(String... parameters)
	{
		return new FailAuthnAction(parameters, getActionType());
	}
	
	public static class FailAuthnAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FailAuthnAction.class);
		private String error;

		public FailAuthnAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			log.debug("Authentication will be failed with message: " + error);
			throw new ExecutionFailException(error);
		}

		private void setParameters(String[] parameters)
		{
			error = parameters[0];
		}
	}
}
