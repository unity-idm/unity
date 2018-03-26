/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.form.GroupParam;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for removing a requested group membership from the request
 * 
 * @author K. Benedyczak
 */
@Component
public class FilterGroupActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "regFilterGroup";
	
	public FilterGroupActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("group", 
						"RegTranslationAction.regFilterGroup.paramDesc.group",
						Type.EXPRESSION, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new FilterGroupAction(getActionType(), parameters);
	}
	
	public static class FilterGroupAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				FilterGroupActionFactory.FilterGroupAction.class);
		private Pattern groupPattern;
		
		public FilterGroupAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Set<GroupParam> copy = new HashSet<GroupParam>(state.getGroups());
			for (GroupParam g: copy)
				if (groupPattern.matcher(g.getGroup()).matches())
				{
					log.debug("Filtering group " + g.getGroup());
					state.removeMembership(g.getGroup());
				}
		}
		
		private void setParameters(String[] parameters)
		{
			groupPattern = Pattern.compile(parameters[0]);
		}
	}
}
