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
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.ActionParameterDesc.Type;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;

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
		super(NAME, new ActionParameterDesc(
				"attribute",
				"TranslationAction.filterAttribute.paramDesc.attributeRegexp",
				Type.EXPRESSION));
	}
	
	@Override
	public FilterAttributeAction getInstance(String... parameters)
	{
		return new FilterAttributeAction(parameters, this);
	}
	
	public static class FilterAttributeAction extends AbstractOutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FilterAttributeAction.class);
		private Pattern attrPattern;

		public FilterAttributeAction(String[] params, TranslationActionDescription desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Set<Attribute<?>> copy = new HashSet<Attribute<?>>(result.getAttributes());
			for (Attribute<?> a: copy)
				if (attrPattern.matcher(a.getName()).matches())
				{
					log.debug("Filtering the attribute " + a.getName());
					result.getAttributes().remove(a);
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
