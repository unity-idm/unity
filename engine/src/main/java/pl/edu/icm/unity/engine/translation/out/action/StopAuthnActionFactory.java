/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.time.Duration;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.engine.api.translation.StopAuthenticationException;
import pl.edu.icm.unity.engine.api.translation.out.AuthenticationFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;

@Component
public class StopAuthnActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "stopAuthentication";

	public StopAuthnActionFactory()
	{
		super(NAME,
				new ActionParameterDefinition("title", "TranslationAction.stopAuthentication.paramDesc.title",
						Type.I18N_TEXT, false),
				new ActionParameterDefinition("info", "TranslationAction.stopAuthentication.paramDesc.info",
						Type.I18N_TEXT, false),
				new ActionParameterDefinition("redirectURL",
						"TranslationAction.stopAuthentication.paramDesc.redirectURL", Type.TEXT, false),
				new ActionParameterDefinition("redirectCaption",
						"TranslationAction.stopAuthentication.paramDesc.redirectCaption", Type.I18N_TEXT, false),
				new ActionParameterDefinition("redirectAfter",
						"TranslationAction.stopAuthentication.paramDesc.redirectAfter", Type.INTEGER, false));
	}

	@Override
	public StopAuthnAction getInstance(String... parameters)
	{
		return new StopAuthnAction(parameters, getActionType());
	}

	public static class StopAuthnAction extends OutputTranslationAction
	{
		private I18nString title;
		private I18nString info;
		private I18nString redirectCaption;
		private String redirectURL;
		private int redirectAfter;
		
		public StopAuthnAction(String[] params, TranslationActionType desc)
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			throw new StopAuthenticationException(AuthenticationFinalizationConfiguration.builder()
					.withInfo(info)
					.withTitle(title)
					.withRedirectCaption(redirectCaption)
					.withRedirectURL(redirectURL != null && !redirectURL.isEmpty() ? redirectURL : null)
					.withRedirectAfterTime(Duration.ofSeconds(redirectAfter))
					.build());

		}

		private void setParameters(String[] parameters)
		{
			try
			{
				title = Constants.MAPPER.readValue(parameters[0], I18nString.class);
				info = Constants.MAPPER.readValue(parameters[1], I18nString.class);
				redirectCaption = Constants.MAPPER.readValue(parameters[3], I18nString.class);

			} catch (Exception e)
			{
				throw new IllegalArgumentException(
						"Action parameter is not a " + "valid JSON representation of i18n string", e);
			}
			redirectURL = parameters[2];
			redirectAfter = Integer.valueOf(parameters[4]);
		}
	}
}
