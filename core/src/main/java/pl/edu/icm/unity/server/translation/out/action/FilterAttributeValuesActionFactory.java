/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out.action;

import java.util.HashSet;
import java.util.List;
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
 * Filter outgoing attribute values.
 *   
 * @author K. Benedyczak
 */
@Component
public class FilterAttributeValuesActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "filterAttributeValues";
	
	public FilterAttributeValuesActionFactory()
	{
		super(NAME, new ActionParameterDesc(
				"attribute",
				"TranslationAction.filterAttributeValue.paramDesc.attribute",
				Type.UNITY_ATTRIBUTE),
		new ActionParameterDesc(
				"attributeValueRegexp",
				"TranslationAction.filterAttributeValue.paramDesc.attributeValueRegexp",
				Type.EXPRESSION));
	}
	
	@Override
	public FilterAttributeValuesAction getInstance(String... parameters)
	{
		return new FilterAttributeValuesAction(parameters, this);
	}
	
	public static class FilterAttributeValuesAction extends AbstractOutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FilterAttributeValuesAction.class);
		private String attr;
		private Pattern pattern;

		public FilterAttributeValuesAction(String[] params, TranslationActionDescription desc) 
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
				if (attr.equals(a.getName()))
				{
					List<?> values = a.getValues();
					int orig = values.size();
					for (int i=values.size()-1; i>=0; i--)
					{
						if (pattern.matcher(values.get(i).toString()).matches())
							values.remove(i);
					}
					if (orig != values.size())
					{
						log.debug("Filtering the values of attribute " + a.getName());
					}
				}
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			attr = parameters[0];
			pattern = Pattern.compile(parameters[1]);
		}

	}
}
