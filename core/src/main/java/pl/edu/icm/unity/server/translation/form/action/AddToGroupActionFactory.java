/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import java.io.Serializable;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for adding a requester to additional group
 * 
 * @author K. Benedyczak
 */
@Component
public class AddToGroupActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "addToGroup";
	
	public AddToGroupActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("group", 
						"RegTranslationAction.addToGroup.paramDesc.group", 
						Type.EXPRESSION),
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AddToGroupAction(getActionType(), parameters);
	}
	
	public static class AddToGroupAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				AddToGroupActionFactory.AddToGroupAction.class);
		private Serializable expression;
		
		public AddToGroupAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Object result = MVEL.executeExpression(expression, mvelCtx);
			if (result == null)
			{
				log.debug("Group evaluated to null, skipping");
				return;
			}
			if (result instanceof Collection<?>)
			{
				Collection<?> mgs = (Collection<?>) result;
				for (Object mg: mgs)
				{
					log.debug("Mapped group: " + mg.toString());
					state.addMembership(new GroupParam(mg.toString(), null,
							currentProfile));
				}
			} else
			{
				log.debug("Mapped group: " + result.toString());
				state.addMembership(new GroupParam(result.toString(), null,
						currentProfile));
			}
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			expression = MVEL.compileExpression(parameters[0]);
		}
	}
}
