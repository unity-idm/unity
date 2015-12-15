/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;

/**
 * Allows for auto processing (accept, deny, drop) of a request.
 * 
 * @author K. Benedyczak
 */
@Component
public class AutoProcessActionFactory extends AbstractTranslationActionFactory
{
	public static final String NAME = "autoProcess";
	
	public AutoProcessActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc("action", 
						"TranslationAction.autoProcess.paramDesc.action", 1, 1, 
						AutomaticRequestAction.class)
		});
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new AutoProcessAction(this, parameters);
	}
	
	public static class AutoProcessAction extends AbstractRegistrationTranslationAction
	{
		private AutomaticRequestAction action;
		
		public AutoProcessAction(TranslationActionDescription description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			state.setAutoAction(action);
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			action = AutomaticRequestAction.valueOf(parameters[0]);
		}
	}

}
