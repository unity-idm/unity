/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;

/**
 * Sets a non-default credential requirement for the requester.
 * @author K. Benedyczak
 */
@Component
public class SetCredentialRequirementActionFactory extends AbstractTranslationActionFactory
{
	public static final String NAME = "setCredReq";
	
	public SetCredentialRequirementActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc(
						"credential requirement",
						"RegTranslationAction.setCredReq.paramDesc.credentialRequirement",
						Type.UNITY_CRED_REQ)
		});
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new SetCredentialRequirementAction(this, parameters);
	}
	
	public static class SetCredentialRequirementAction extends AbstractRegistrationTranslationAction
	{
		private String credReq;
		
		public SetCredentialRequirementAction(TranslationActionDescription description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			state.setCredentialRequirement(credReq);
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			credReq = parameters[0];
		}
	}
}
