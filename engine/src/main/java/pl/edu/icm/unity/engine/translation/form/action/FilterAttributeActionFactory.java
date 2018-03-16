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
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for removing a requested attribute from the request
 * 
 * @author K. Benedyczak
 */
@Component(FilterAttributeActionFactory.NAME)
public class FilterAttributeActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "regFilterAttribute";
	
	public FilterAttributeActionFactory()
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition("attribute", 
						"RegTranslationAction.regFilterAttribute.paramDesc.attribute",
						Type.EXPRESSION, true),
				new ActionParameterDefinition("group", 
						"RegTranslationAction.regFilterAttribute.paramDesc.group",
						Type.UNITY_GROUP, true)
		});
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new FilterAttributeAction(getActionType(), parameters);
	}
	
	public static class FilterAttributeAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				FilterAttributeActionFactory.FilterAttributeAction.class);
		private Pattern attributePattern;
		private String group;
		
		public FilterAttributeAction(TranslationActionType description, String[] parameters)
		{
			super(description, parameters);
			setParameters(parameters);
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Set<Attribute> copy = new HashSet<>(state.getAttributes());
			for (Attribute a: copy)
				if (attributePattern.matcher(a.getName()).matches() && a.getGroupPath().equals(group))
				{
					log.debug("Filtering attribute " + a);
					state.removeAttribute(a);
				}
		}
		
		private void setParameters(String[] parameters)
		{
			attributePattern = Pattern.compile(parameters[0]);
			group = parameters[1];
		}
	}
}
