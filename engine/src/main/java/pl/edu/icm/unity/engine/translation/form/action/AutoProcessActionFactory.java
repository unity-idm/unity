/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for auto processing (accept, deny, drop) of a request.
 * 
 * @author K. Benedyczak
 */
@Component
public class AutoProcessActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "autoProcess";
	
	public AutoProcessActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("action", 
						"RegTranslationAction.autoProcess.paramDesc.action", 
						AutomaticRequestAction.class, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AutoProcessAction(getActionType(), parameters);
	}
	
	public static class AutoProcessAction extends RegistrationTranslationAction
	{
		private AutomaticRequestAction action;
		
		public AutoProcessAction(TranslationActionType description, String[] parameters)
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
			action = AutomaticRequestAction.valueOf(parameters[0]);
		}
	}

}
