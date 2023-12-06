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
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.GroupRestrictedFormValidationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
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
				new ActionParameterDefinition("group", "RegTranslationAction.addToGroup.paramDesc.group",
						Type.EXPRESSION, true,
						MVELExpressionContext.builder().withTitleKey("RegTranslationAction.addToGroup.editor.title")
								.withEvalToKey("RegTranslationAction.addIdentity.addToGroup.editor.evalTo")
								.withVars(RegistrationMVELContextKey.toMap()).build()),
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
		private String rawExpression;
		
		public AddToGroupAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				RegistrationContext context, String currentProfile) throws EngineException
		{
			Object result = MVEL.executeExpression(expression, mvelCtx, new HashMap<>());
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
		
		@Override
		public void validateGroupRestrictedForm(GroupRestrictedFormValidationContext context) throws ActionValidationException
		{
			if (!((rawExpression.startsWith("\"") || rawExpression.startsWith("'"))
					&& (rawExpression.endsWith("\"") || rawExpression.endsWith("'"))))
			{
				throw new ActionValidationException("Only literal expression is allowed in form automation addToGroup action");
			}
			String group = rawExpression.substring(1, rawExpression.length() -1);	
			if (!Group.isChildOrSame(group, context.parentGroup))
			{
				throw new ActionValidationException("Group " + group + " is not allowed in form automation addToGroup action");
			}
		}
		
		private void setParameters(String[] parameters)
		{
			expression = MVEL.compileExpression(parameters[0]);
			rawExpression = parameters[0];
		}
	}
}
