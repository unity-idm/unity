/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Creates new outgoing attributes which are not persisted locally.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreateAttributeActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "createAttribute";
	
	public CreateAttributeActionFactory()
	{
		super(NAME, new ActionParameterDefinition(
				"attributeName",
				"TranslationAction.createAttribute.paramDesc.attributeName",
				Type.EXPRESSION),
		new ActionParameterDefinition(
				"expression",
				"TranslationAction.createAttribute.paramDesc.expression",
				Type.EXPRESSION));
	}
	
	@Override
	public CreateAttributeAction getInstance(String... parameters)
	{
		return new CreateAttributeAction(parameters, getActionType());
	}
	
	public static class CreateAttributeAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreateAttributeAction.class);
		private String attrNameString;
		private Serializable valuesExpression;

		public CreateAttributeAction(String[] params, TranslationActionType desc) 
		{
			super(desc, params);
			setParameters(params);
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
				TranslationResult result) throws EngineException
		{
			Object value = MVEL.executeExpression(valuesExpression, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Attribute value evaluated to null, skipping");
				return;
			}
			for (Attribute existing: result.getAttributes())
			{
				if (existing.getName().equals(attrNameString))
				{
					log.trace("Attribute already exists, skipping");
					return;
				}
			}
			List<?> values;		
			if (value instanceof List)
				values = (List<?>) value;
			else
				values = Collections.singletonList(value);
			
			List<String> sValues = new ArrayList<>(values.size());
			for (Object v: values)
				sValues.add(v.toString());
			
			Attribute newAttr = new Attribute(attrNameString, StringAttributeSyntax.ID, "/", sValues);
			result.getAttributes().add(newAttr);
			log.debug("Created a new attribute: " + newAttr);
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException("Action requires exactly 2 parameters");
			attrNameString = parameters[0];
			valuesExpression = MVEL.compileExpression(parameters[1]);
		}

	}
}
