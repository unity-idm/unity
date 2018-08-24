/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.translation.form.AutomaticInvitationProcessingParam;
import pl.edu.icm.unity.engine.api.translation.form.AutomaticInvitationProcessingParam.InvitationProcessingMode;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

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
				new ActionParameterDefinition("formWithInvitation", 
						"RegTranslationAction.autoProcessInvitations.paramDesc.formWithInvitation", 
						Type.REGISTRATION_FORM_WITH_INVITATION, true),
				new ActionParameterDefinition("processingMode", 
						"RegTranslationAction.autoProcessInvitations.paramDesc.processingMode", 
						InvitationProcessingMode.class, true)
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
				String currentProfile) throws EngineException
		{
			state.setInvitationProcessing(invitationProcessing);
		}

		private void setParameters(String[] parameters)
		{
			String formName = parameters[0];
			InvitationProcessingMode mode = InvitationProcessingMode.valueOf(parameters[1]);
			this.invitationProcessing = new AutomaticInvitationProcessingParam(formName, mode);
		}
	}
}
