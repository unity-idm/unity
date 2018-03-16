/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

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
		super(NAME, new ActionParameterDefinition(
				"attribute",
				"TranslationAction.filterAttributeValue.paramDesc.attribute",
				Type.UNITY_ATTRIBUTE, true),
		new ActionParameterDefinition(
				"attributeValueRegexp",
				"TranslationAction.filterAttributeValue.paramDesc.attributeValueRegexp",
				Type.EXPRESSION, true));
	}
	
	@Override
	public FilterAttributeValuesAction getInstance(String... parameters)
	{
		return new FilterAttributeValuesAction(parameters, getActionType());
	}
	
	public static class FilterAttributeValuesAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, FilterAttributeValuesAction.class);
		private String attr;
		private Pattern pattern;

		public FilterAttributeValuesAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx,
				String currentProfile, TranslationResult result)
				throws EngineException
		{
			for (DynamicAttribute a : result.getAttributes())
			{
				String attrName = a.getAttribute().getName();

				if (attr.equals(attrName))
				{
					if (filterValue(a.getAttribute()))
					{
						log.debug("Filtering the values of attribute "
								+ attrName);
					}
				}
			}

			for (Attribute a : result.getAttributesToPersist())
			{
				String attrName = a.getName();

				if (attr.equals(attrName))
				{
					if (filterValue(a))
					{
						log.debug("Filtering the values of attribute to persist "
								+ attrName);
					}
				}
			}
		}

		private boolean filterValue(Attribute a)
		{
			boolean res = false;
			List<?> values = a.getValues();
			int orig = values.size();
			for (int i = values.size() - 1; i >= 0; i--)
			{
				if (pattern.matcher(values.get(i).toString()).matches())
					values.remove(i);
			}
			if (orig != values.size())
			{
				res = true;
			}
			return res;
		}

		private void setParameters(String[] parameters)
		{
			attr = parameters[0];
			pattern = Pattern.compile(parameters[1]);
		}

	}
}
