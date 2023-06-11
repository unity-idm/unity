/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;

/**
 * Sets a given MFA-use opt-in status for the user 
 */
@Component
public class SetEntityMFAActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "setMFAPreference";

	public SetEntityMFAActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"enabled",
						"RegTranslationAction.setMFAPreference.paramDesc.enabled",
						Type.BOOLEAN, 
						true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new SetEntityMFAAction(getActionType(), parameters);
	}
	
	public static class SetEntityMFAAction extends RegistrationTranslationAction
	{
		private boolean enable;
		
		public SetEntityMFAAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				RegistrationContext context, String currentProfile) throws EngineException
		{
			state.setMfaPreferenceStatus(enable);
		}
		
		private void setParameters(String[] parameters)
		{
			enable = Boolean.valueOf(parameters[0]);
		}
	}
}
