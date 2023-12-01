/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.form.GroupRestrictedFormValidationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;

/**
 * Sets a non-default credential requirement for the requester.
 * @author K. Benedyczak
 */
@Component
public class SetCredentialRequirementActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "setCredReq";
	
	public SetCredentialRequirementActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"credential requirement",
						"RegTranslationAction.setCredReq.paramDesc.credentialRequirement",
						Type.UNITY_CRED_REQ, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new SetCredentialRequirementAction(getActionType(), parameters);
	}
	
	public static class SetCredentialRequirementAction extends RegistrationTranslationAction
	{
		private String credReq;
		
		public SetCredentialRequirementAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				RegistrationContext context, String currentProfile) throws EngineException
		{
			state.setCredentialRequirement(credReq);
		}
		
		@Override
		public void validateGroupRestrictedForm(GroupRestrictedFormValidationContext context) throws ActionValidationException
		{
		}
		
		private void setParameters(String[] parameters)
		{
			credReq = parameters[0];
		}
	}
}
