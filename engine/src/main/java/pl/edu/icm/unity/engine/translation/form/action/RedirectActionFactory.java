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
 * Factory, creating actions which can be used to redirect the request submitter,
 * immediately after form submission.
 * 
 * @author K. Benedyczak
 */
@Component
@Deprecated
public class RedirectActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "redirect";
	
	public RedirectActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("URL", 
						"RegTranslationAction.redirect.paramDesc.URL",
						Type.EXPRESSION, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new RedirectAction(getActionType(), parameters);
	}
	
	@Deprecated
	public static class RedirectAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				RedirectActionFactory.RedirectAction.class);
		
		public RedirectAction(TranslationActionType description, String[] params)
		{
			super(description, params);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			log.error("The redirect form action is effect less. Please reconfigure your form to use Finalization config instead.");
		}
	}
}



