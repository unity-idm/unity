/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out.action;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;

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
		super(NAME, new ActionParameterDesc(
				"attribute",
				"TranslationAction.unfilterAttribute.paramDesc.attributeRegexp",
				Type.EXPRESSION));
	}
	
	@Override
	public UnFilterAttributeAction getInstance(String... parameters) throws EngineException
	{
		return new UnFilterAttributeAction(parameters, this);
	}

	public static class UnFilterAttributeAction extends AbstractOutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, UnFilterAttributeAction.class);
		private Pattern attrPattern;

		public UnFilterAttributeAction(String[] params, TranslationActionDescription desc) 
				throws EngineException
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Set<String> existing = new HashSet<>();
			for (Attribute<?> a: result.getAttributes())
				existing.add(a.getName());
			
			for (Attribute<?> a: input.getAttributes())
				if (attrPattern.matcher(a.getName()).matches())
				{
					log.debug("Unfiltering the attribute " + a.getName());
					result.getAttributes().add(a);
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
