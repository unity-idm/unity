/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.engine.api.translation.form.AutomaticInvitationProcessingParam;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;

/**
 * Allows for automatic invitation processing.
 * 
 * @author Roman Krysinski
 */
@Component
public class AutoProcessInvitationsActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "autoProcessInvitations";
	
	@Autowired
	public AutoProcessInvitationsActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("registrationForm", 
						"RegTranslationAction.autoProcessInvitations.paramDesc.registrationForm", 
						Type.REGISTRATION_FORM, false)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AutoProcessInvitationsAction(getActionType(), parameters);
	}
	
	public static class AutoProcessInvitationsAction extends RegistrationTranslationAction
	{
		private AutomaticInvitationProcessingParam invitationProcessing;
		
		public AutoProcessInvitationsAction(TranslationActionType description, String[] parameters) 
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				RegistrationContext context, String currentProfile) throws EngineException
		{
			state.addInvitationProcessingParam(invitationProcessing);
		}

		private void setParameters(String[] parameters)
		{
			String formName = parameters[0];
			this.invitationProcessing = new AutomaticInvitationProcessingParam(formName);
		}
	}
}
