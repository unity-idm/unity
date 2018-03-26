/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for assigning an attribute class to requester
 * 
 * @author K. Benedyczak
 */
@Component
public class AddAttributeClassActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "addAttributeClass";
	
	public AddAttributeClassActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("group", 
						"RegTranslationAction.addAttributeClass.paramDesc.group", 
						Type.UNITY_GROUP, true),
				new ActionParameterDefinition("attribute class", 
						"RegTranslationAction.addAttributeClass.paramDesc.ac", 
						Type.EXPRESSION, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AddAttributeClassAction(getActionType(), parameters);
	}
	
	public static class AddAttributeClassAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				AddAttributeClassActionFactory.AddAttributeClassAction.class);
		private Serializable expression;
		private String group;
		
		public AddAttributeClassAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Object result = MVEL.executeExpression(expression, mvelCtx, new HashMap<>());
			if (result == null)
			{
				log.debug("AC evaluated to null, skipping");
				return;
			}
			if (result instanceof Collection<?>)
			{
				Collection<?> mgs = (Collection<?>) result;
				for (Object mg: mgs)
				{
					log.debug("Adding to class: " + mg.toString());
					state.addAttributeClass(group, mg.toString());
				}
			} else
			{
				log.debug("Adding to class: " + result.toString());
				state.addAttributeClass(group, result.toString());
			}
		}
		
		private void setParameters(String[] parameters)
		{
			group = parameters[0];
			expression = MVEL.compileExpression(parameters[1]);
		}
	}
}
