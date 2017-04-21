/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Filter outgoing attributes by name
 *   
 * @author K. Benedyczak
 */
@Component
public class FilterAttributeActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "filterAttribute";
	
	public FilterAttributeActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"attribute",
				"TranslationAction.filterAttribute.paramDesc.attributeRegexp",
				Type.EXPRESSION));
	}
	
	@Override
	public FilterAttributeAction getInstance(String... parameters)
	{
		return new FilterAttributeAction(parameters, getActionType());
	}
	
	public static class FilterAttributeAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FilterAttributeAction.class);
		private Pattern attrPattern;

		public FilterAttributeAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Set<DynamicAttribute> copy = new HashSet<DynamicAttribute>(result.getAttributes());
			for (DynamicAttribute a: copy)
			{
				String attrName = a.getAttribute().getName();
				if (attrPattern.matcher(attrName).matches())
				{
					log.debug("Filtering the attribute " + attrName);
					result.getAttributes().remove(a);
				}
			}
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			attrPattern = Pattern.compile(parameters[0]);
		}

	}

}
