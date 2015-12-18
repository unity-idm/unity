/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Allows for removing a requested group membership from the request
 * 
 * @author K. Benedyczak
 */
@Component
public class FilterGroupActionFactory extends AbstractTranslationActionFactory
{
	public static final String NAME = "regFilterGroup";
	
	public FilterGroupActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc("group", 
						"RegTranslationAction.filterGroup.paramDesc.group",
						Type.EXPRESSION)
		});
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new FilterGroupAction(this, parameters);
	}
	
	public static class FilterGroupAction extends AbstractRegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				FilterGroupActionFactory.FilterGroupAction.class);
		private Pattern groupPattern;
		
		public FilterGroupAction(TranslationActionDescription description, String[] parameters)
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
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			groupPattern = Pattern.compile(parameters[0]);
		}
	}
}
