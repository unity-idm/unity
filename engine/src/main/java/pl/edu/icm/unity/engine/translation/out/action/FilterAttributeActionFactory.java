/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Filter outgoing attributes by name
 *   
 * @author K. Benedyczak
 */
@Component(FilterAttributeActionFactory.NAME)
public class FilterAttributeActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "filterAttribute";
	
	public FilterAttributeActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"attribute",
				"TranslationAction.filterAttribute.paramDesc.attributeRegexp",
				Type.TEXT, true));
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
			Set<String> res = result.removeAttributesByMatch(attrPattern);
			if (!res.isEmpty())
			{
				for (String attrName : res)
					log.debug("Filtering the attribute " + attrName);
			}
			res = result.removeAttributesToPersistByMatch(attrPattern);
			if (!res.isEmpty())
			{
				for (String attrName : res)
					log.debug("Filtering the attribute to persist " + attrName);
			}
			
		}

		private void setParameters(String[] parameters)
		{
			attrPattern = Pattern.compile(parameters[0]);
		}

	}

}
