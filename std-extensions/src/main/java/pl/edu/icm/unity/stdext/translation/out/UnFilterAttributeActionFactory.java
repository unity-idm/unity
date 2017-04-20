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
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Inserts previously filtered outgoing attributes by name
 *   
 * @author K. Benedyczak
 */
@Component
public class UnFilterAttributeActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "unfilterAttribute";
	
	public UnFilterAttributeActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"attribute",
				"TranslationAction.unfilterAttribute.paramDesc.attributeRegexp",
				Type.EXPRESSION));
	}
	
	@Override
	public UnFilterAttributeAction getInstance(String... parameters)
	{
		return new UnFilterAttributeAction(parameters, getActionType());
	}

	public static class UnFilterAttributeAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, UnFilterAttributeAction.class);
		private Pattern attrPattern;

		public UnFilterAttributeAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Set<String> existing = new HashSet<>();
			for (DynamicAttribute a: result.getAttributes())
				existing.add(a.getAttribute().getName());
			
			for (Attribute<?> a: input.getAttributes())
				if (attrPattern.matcher(a.getName()).matches())
				{
					log.debug("Unfiltering the attribute " + a.getName());
					result.getAttributes().add(new DynamicAttribute(a));
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
