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
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Allows for removing a requested attribute from the request
 * 
 * @author K. Benedyczak
 */
@Component
public class FilterAttributeActionFactory extends AbstractTranslationActionFactory
{
	public static final String NAME = "regFilterAttribute";
	
	public FilterAttributeActionFactory()
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc("attribute", 
						"RegTranslationAction.filterAttribute.paramDesc.attribute",
						Type.EXPRESSION),
				new ActionParameterDesc("group", 
						"RegTranslationAction.filterAttribute.paramDesc.group",
						Type.UNITY_GROUP)
		});
	}

	@Override
	public TranslationAction getInstance(String... parameters)
	{
		return new FilterAttributeAction(this, parameters);
	}
	
	public static class FilterAttributeAction extends AbstractRegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				FilterAttributeActionFactory.FilterAttributeAction.class);
		private Pattern attributePattern;
		private String group;
		
		public FilterAttributeAction(TranslationActionDescription description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Set<Attribute<?>> copy = new HashSet<>(state.getAttributes());
			for (Attribute<?> a: copy)
				if (attributePattern.matcher(a.getName()).matches() && a.getGroupPath().equals(group))
				{
					log.debug("Filtering attribute " + a);
					state.removeAttribute(a);
				}
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			attributePattern = Pattern.compile(parameters[0]);
			group = parameters[1];
		}
	}
}
