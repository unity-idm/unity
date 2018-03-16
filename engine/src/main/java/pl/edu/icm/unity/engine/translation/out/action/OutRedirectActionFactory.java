/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;


/**
 * Factory, creating actions which can be used to redirect the request submitter,
 * immediately after output translation profile execution.
 * 
 * @author K. Benedyczak
 */
@Component
public class OutRedirectActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "out-redirect";
	
	public OutRedirectActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("URL", 
						"TranslationAction.out-redirect.paramDesc.URL",
						Type.EXPRESSION, true)
		});
	}

	@Override
	public OutputTranslationAction getInstance(String... parameters)
	{
		return new RedirectAction(getActionType(), parameters);
	}
	
	public static class RedirectAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				OutRedirectActionFactory.RedirectAction.class);
		private Serializable urlExpression;
		
		public RedirectAction(TranslationActionType description, String[] params)
		{
			super(description, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx,
				String currentProfile, TranslationResult result) throws EngineException
		{
			Object value = MVEL.executeExpression(urlExpression, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Redirect URL expression evaluated to null, skipping");
				return;
			}
			
			result.setRedirectURL(value.toString());
		}
		
		private void setParameters(String[] parameters)
		{
			urlExpression = MVEL.compileExpression(parameters[0]);
		}
	}
}



