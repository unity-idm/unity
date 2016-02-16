/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;


/**
 * Factory, creating actions which can be used to set a customized after-submission message.
 * 
 * @author K. Benedyczak
 */
@Component
public class SubmitMessageActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "submissionMessage";
	
	public SubmitMessageActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("caption", 
						"RegTranslationAction.submissionMessage.paramDesc.caption",
						Type.I18N_TEXT),
				new ActionParameterDefinition("message", 
						"RegTranslationAction.submissionMessage.paramDesc.message",
						Type.I18N_TEXT)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new SubmitMessageAction(getActionType(), parameters);
	}
	
	public static class SubmitMessageAction extends RegistrationTranslationAction
	{
		private I18nString message;
		private I18nString caption;
		
		public SubmitMessageAction(TranslationActionType description, String[] params)
		{
			super(description, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			state.setPostSubmitMessage(new I18nMessage(caption, message));
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			try
			{
				caption = Constants.MAPPER.readValue(parameters[0], I18nString.class);
				message = Constants.MAPPER.readValue(parameters[1], I18nString.class);
			} catch (Exception e)
			{
				throw new IllegalArgumentException("Action parameter is not a "
						+ "valid JSON representation of i18n string", e);
			}
		}
	}
}



