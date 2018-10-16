/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;


/**
 * Factory, creating actions which can be used to set a customized after-submission message.
 * 
 * @author K. Benedyczak
 */
@Component
@Deprecated
public class SubmitMessageActionFactory extends AbstractRegistrationTranslationActionFactory
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, SubmitMessageActionFactory.class);
	public static final String NAME = "submissionMessage";
	
	public SubmitMessageActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("caption", 
						"RegTranslationAction.submissionMessage.paramDesc.caption",
						Type.I18N_TEXT, true),
				new ActionParameterDefinition("message", 
						"RegTranslationAction.submissionMessage.paramDesc.message",
						Type.I18N_TEXT, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new SubmitMessageAction(getActionType(), parameters);
	}
	
	public static class SubmitMessageAction extends RegistrationTranslationAction
	{
		public SubmitMessageAction(TranslationActionType description, String[] params)
		{
			super(description, params);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			log.error("The submissionMessage form action is effectless. Please reconfigure your form to use Finalization config instead.");
		}
	}
}



