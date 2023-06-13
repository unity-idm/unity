/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;

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
				Type.EXPRESSION, true,
				MVELExpressionContext.builder().withTitleKey("TranslationAction.unfilterAttribute.editor.title")
					.withEvalToKey("TranslationAction.unfilterAttribute.editor.evalTo")
					.withVars(OutputTranslationMVELContextKey.toMap()).build()));
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
			
			
			for (Attribute a : input.getAttributes())
			{
				String attrName = a.getName();
				if (attrPattern.matcher(attrName).matches())
				{
					log.debug("Unfiltering the attribute " + attrName);
					result.removeAttributesByName(attrName);
					result.removeAttributesToPersistByName(attrName);
					result.getAttributes().add(new DynamicAttribute(a));
				}
			}
		}

		private void setParameters(String[] parameters)
		{
			attrPattern = Pattern.compile(parameters[0]);
		}
	}
}
