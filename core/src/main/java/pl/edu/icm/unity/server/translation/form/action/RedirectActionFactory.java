/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;
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
public class RedirectActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "redirect";
	
	public RedirectActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("URL", 
						"RegTranslationAction.redirect.paramDesc.URL",
						Type.EXPRESSION)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new RedirectAction(getActionType(), parameters);
	}
	
	public static class RedirectAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				RedirectActionFactory.RedirectAction.class);
		private Serializable urlExpression;
		
		public RedirectAction(TranslationActionType description, String[] params)
		{
			super(description, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Object value = MVEL.executeExpression(urlExpression, mvelCtx);
			if (value == null)
			{
				log.debug("Redirect URL expression evaluated to null, skipping");
				return;
			}
			
			state.setRedirectURL(value.toString());
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			urlExpression = MVEL.compileExpression(parameters[0]);
		}
	}
}



