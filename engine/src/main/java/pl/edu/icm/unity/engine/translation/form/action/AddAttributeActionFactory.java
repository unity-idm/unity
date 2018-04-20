/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for adding an additional attribute to for the requester
 * 
 * @author K. Benedyczak
 */
@Component
public class AddAttributeActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "addAttribute";
	private AttributeTypeSupport attrsSupport;
	private AttributeValueConverter attrValueConverter;
	
	@Autowired
	public AddAttributeActionFactory(AttributeTypeSupport attrsSupport, AttributeValueConverter attrValueConverter)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"attributeName",
						"RegTranslationAction.addAttribute.paramDesc.attributeName",
						Type.UNITY_ATTRIBUTE, true),
				new ActionParameterDefinition(
						"group",
						"RegTranslationAction.addAttribute.paramDesc.group",
						Type.UNITY_GROUP, true),
				new ActionParameterDefinition(
						"expression",
						"RegTranslationAction.addAttribute.paramDesc.expression",
						Type.EXPRESSION, true)
		});
		this.attrsSupport = attrsSupport;
		this.attrValueConverter = attrValueConverter;
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AddAttributeAction(getActionType(), parameters, attrsSupport, attrValueConverter);
	}
	
	public static class AddAttributeAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				AddAttributeActionFactory.AddAttributeAction.class);
		private String unityAttribute;
		private String group;
		private Serializable expressionCompiled;
		private AttributeType at;
		private AttributeValueConverter attrValueConverter;
		
		public AddAttributeAction(TranslationActionType description, String[] parameters, 
				AttributeTypeSupport attrsSupport, AttributeValueConverter attrValueConverter)
		{
			super(description, parameters);
			this.attrValueConverter = attrValueConverter;
			setParameters(parameters);
			at = attrsSupport.getType(unityAttribute);
			if (at == null)
				throw new IllegalArgumentException(
						"Attribute type " + unityAttribute + " is not known");
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx,
				String currentProfile) throws EngineException
		{
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Attribute value evaluated to null, skipping");
				return;
			}
			
			List<?> aValues = value instanceof List ? (List<?>)value : Collections.singletonList(value);
			List<String> typedValues;
			try
			{
				typedValues = attrValueConverter.externalValuesToInternal(unityAttribute, aValues);
			} catch (IllegalAttributeValueException e)
			{
				log.debug("Can't convert attribute values returned by the action's expression "
						+ "to the type of attribute " + unityAttribute + " , skipping it", e);
				return;
			}
			
			Attribute attribute = new Attribute(unityAttribute, at.getValueSyntax(), group, 
					typedValues, null, currentProfile);
			log.debug("Mapped attribute: " + attribute);
			state.addAttribute(attribute);
		}

		private void setParameters(String[] parameters)
		{
			unityAttribute = parameters[0];
			group = parameters[1];
			expressionCompiled = MVEL.compileExpression(parameters[2]);
		}
	}
}
