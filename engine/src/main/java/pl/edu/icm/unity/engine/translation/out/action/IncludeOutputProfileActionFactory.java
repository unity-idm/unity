/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out.action;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
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
public class IncludeOutputProfileActionFactory extends AbstractOutputTranslationActionFactory
{

	public static final String NAME = "includeOutputProfile";

	@Autowired
	public IncludeOutputProfileActionFactory()
	{
		super(NAME, new ActionParameterDefinition("outputProfile",
				"TranslationAction.includeOutputProfile.paramDesc.outputProfile",
				Type.UNITY_OUTPUT_TRANSLATION_PROFILE, true));
	}

	@Override
	public OutputTranslationAction getInstance(String... parameters)
	{
		return new IncludeOutputProfileAction(parameters, getActionType());
	}

	public static class IncludeOutputProfileAction extends OutputTranslationAction
			implements TranslationIncludeProfileAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				IncludeOutputProfileAction.class);

		private String profile;

		public IncludeOutputProfileAction(String[] parameters,
				TranslationActionType actionType)
		{
			super(actionType, parameters);
			setParameters(parameters);
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

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx,
				String currentProfile, TranslationResult result)
				throws EngineException
		{
			log.debug("Include translation profile '" + profile + "'");

		}

	}
}
