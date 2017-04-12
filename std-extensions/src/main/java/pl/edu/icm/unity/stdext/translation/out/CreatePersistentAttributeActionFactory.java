/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.translation.out;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.translation.in.action.MapAttributeActionFactory;
import pl.edu.icm.unity.server.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Creates new outgoing attributes.
 *   
 * @author K. Benedyczak
 */
@Component
public class CreatePersistentAttributeActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "createPersistentAttribute";
	private AttributesManagement attrsMan;
	
	@Autowired
	public CreatePersistentAttributeActionFactory(@Qualifier("insecure") AttributesManagement attrsMan)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"attributeName",
						"TranslationAction.createPersistentAttribute.paramDesc.attributeName",
						Type.UNITY_ATTRIBUTE),
				new ActionParameterDefinition(
						"expression",
						"TranslationAction.createPersistentAttribute.paramDesc.expression",
						Type.EXPRESSION),
				new ActionParameterDefinition(
						"mandatory",
						"TranslationAction.createPersistentAttribute.paramDesc.mandatory",
						Type.BOOLEAN),
				new ActionParameterDefinition(
						"group",
						"TranslationAction.createPersistentAttribute.paramDesc.group",
						Type.UNITY_GROUP)
		});
		this.attrsMan = attrsMan;
	}

	@Override
	public CreatePersistentAttributeAction getInstance(String... parameters)
	{
		return new CreatePersistentAttributeAction(parameters, getActionType(), attrsMan);
	}
	
	public static class CreatePersistentAttributeAction extends OutputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreatePersistentAttributeAction.class);
		private String attrNameString;
		private AttributeType attributeType;
		private Serializable valuesExpression;
		private String group;
		private boolean attrMandatory;

		public CreatePersistentAttributeAction(String[] params, TranslationActionType desc, 
				AttributesManagement attrsMan)
		{
			super(desc, params);
			setParameters(params, attrsMan);
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
			for (DynamicAttribute existing: result.getAttributes())
			{
				if (existing.getAttribute().getName().equals(attrNameString))
				{
					existing.setMandatory(attrMandatory);
					log.trace("Attribute already exists, skipping");
					return;
				}
			}

			List<Object> typedValues;
			try
			{
				typedValues = MapAttributeActionFactory.MapAttributeAction.convertValues(
						value, attributeType.getValueType());
			} catch (IllegalAttributeValueException e)
			{
				log.debug("Can't convert attribute values returned by the action's expression "
						+ "to the type of attribute " + attrNameString + " , skipping it", e);
				return;
			}
			//for output profile we can't confirm - not yet implemented and rather not needed.
			for (Object val: typedValues)
			{
				if (val instanceof VerifiableElement)
				{
					((VerifiableElement) val).setConfirmationInfo(new ConfirmationInfo(true));
				}
			}

			@SuppressWarnings({ "unchecked", "rawtypes"})
			Attribute<?> newAttr = new Attribute(attrNameString, attributeType.getValueType(), group, 
					AttributeVisibility.full, typedValues, null, currentProfile);
			DynamicAttribute dat = new DynamicAttribute(newAttr);
			dat.setMandatory(attrMandatory);
			result.getAttributes().add(dat);
			result.getAttributesToPersist().add(newAttr);
			log.debug("Created a new persisted attribute: " + dat);
		}

		private void setParameters(String[] parameters, AttributesManagement attrsMan)
		{
			if (parameters.length != 4)
				throw new IllegalArgumentException("Action requires exactly 4 parameters");
			attrNameString = parameters[0];
			valuesExpression = MVEL.compileExpression(parameters[1]);
			attrMandatory = Boolean.valueOf(parameters[2]);
			group = parameters[3];
			
			
			try
			{
				attributeType = attrsMan.getAttributeTypesAsMap().get(attrNameString);
				if (attributeType == null)
					throw new IllegalArgumentException("The attribute type " + parameters[0] + 
							" is not a valid Unity attribute type and therefore can not be persisted");
				if (attributeType.isInstanceImmutable())
					throw new IllegalArgumentException("The attribute type " + parameters[0] + 
							" is managed internally only so it can not be persisted");
			} catch (EngineException e)
			{
				throw new IllegalStateException("Can not verify attribute type", e);
			}
		}

	}
}
