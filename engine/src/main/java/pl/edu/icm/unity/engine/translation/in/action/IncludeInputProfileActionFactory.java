/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.in.action;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.TranslationIncludeProfileAction;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * 
 * Factory for @{IncludeInputProfileAction}
 * 
 * @author P.Piernik
 *
 */
@Component
public class IncludeInputProfileActionFactory extends AbstractInputTranslationActionFactory
{

	public static final String NAME = "includeInputProfile";

	@Autowired
	public IncludeInputProfileActionFactory()
	{
		super(NAME, new ActionParameterDefinition("inputProfile",
				"TranslationAction.includeInputProfile.paramDesc.inputProfile",
				Type.UNITY_INPUT_TRANSLATION_PROFILE, true));
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new IncludeInputProfileAction(parameters, getActionType());
	}

	public static class IncludeInputProfileAction extends InputTranslationAction implements TranslationIncludeProfileAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				IncludeInputProfileAction.class);

		private String profile;

		public IncludeInputProfileAction(String[] parameters,
				TranslationActionType actionType)
		{
			super(actionType, parameters);
			setParameters(parameters);
		}

		@Override
		protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input,
				Object mvelCtx, String currentProfile) throws EngineException
		{
			log.debug("Include translation profile '" + profile + "'");
			MappingResult result = new MappingResult();
			return result;
		}

		private void setParameters(String[] parameters)
		{
			profile = parameters[0];
		}

		@Override
		public String getIncludedProfile()
		{
			return profile;
		}

	}
}
