/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in.action;

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
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Factory for {@link MapAttributeAction}.
 *   
 * @author K. Benedyczak
 */
@Component
public class MapAttributeActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "mapAttribute";
	private AttributeTypeSupport attrsMan;
	private AttributeValueConverter attrValueConverter;
	
	
	@Autowired
	public MapAttributeActionFactory(AttributeTypeSupport attrsMan, AttributeValueConverter attrValueConverter)
	{
		super(NAME, 
			new ActionParameterDefinition(
				"unityAttribute",
				"TranslationAction.mapAttribute.paramDesc.unityAttribute",
				Type.UNITY_ATTRIBUTE, true),
			new ActionParameterDefinition(
				"group",
				"TranslationAction.mapAttribute.paramDesc.group",
				Type.UNITY_GROUP, true),
			new ActionParameterDefinition(
				"expression",
				"TranslationAction.mapAttribute.paramDesc.expression",
				Type.EXPRESSION, true),
			new ActionParameterDefinition(
				"effect",
				"TranslationAction.mapAttribute.paramDesc.effect",
				AttributeEffectMode.class, true));
		
		this.attrsMan = attrsMan;
		this.attrValueConverter = attrValueConverter;
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new MapAttributeAction(parameters, getActionType(), attrsMan, attrValueConverter);
	}
	
	
	public static class MapAttributeAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapAttributeAction.class);
		private String unityAttribute;
		private String group;
		private Serializable expressionCompiled;
		private AttributeEffectMode mode;
		private AttributeType at;
		private AttributeValueConverter attrValueConverter;

		public MapAttributeAction(String[] params, TranslationActionType desc, AttributeTypeSupport attrsMan,
				AttributeValueConverter attrValueConverter) 
		{
			super(desc, params);
			this.attrValueConverter = attrValueConverter;
			setParameters(params);
			at = attrsMan.getType(unityAttribute);
		}
		
		@Override
		protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, 
				String currentProfile)
		{
			MappingResult ret = new MappingResult();
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Attribute value evaluated to null, skipping");
				return ret;
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
				return ret;
			}
			
			Attribute attribute = new Attribute(unityAttribute, at.getValueSyntax(), group, 
					typedValues, input.getIdpName(), currentProfile);
			MappedAttribute ma = new MappedAttribute(mode, attribute);
			log.debug("Mapped attribute: " + attribute);
			ret.addAttribute(ma);
			return ret;
		}

		private void setParameters(String[] parameters)
		{
			unityAttribute = parameters[0];
			group = parameters[1];
			expressionCompiled = MVEL.compileExpression(parameters[2]);
			mode = AttributeEffectMode.valueOf(parameters[3]);
		}
	}
}
